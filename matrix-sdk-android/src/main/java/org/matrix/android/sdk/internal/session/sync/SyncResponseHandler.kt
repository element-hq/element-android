/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.sync

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.extensions.measureMetric
import org.matrix.android.sdk.api.extensions.measureSpan
import org.matrix.android.sdk.api.metrics.SyncDurationMetricPlugin
import org.matrix.android.sdk.api.session.pushrules.PushRuleService
import org.matrix.android.sdk.api.session.pushrules.RuleScope
import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import org.matrix.android.sdk.api.session.sync.model.RoomsSyncResponse
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.SessionListeners
import org.matrix.android.sdk.internal.session.dispatchTo
import org.matrix.android.sdk.internal.session.pushrules.ProcessEventForPushTask
import org.matrix.android.sdk.internal.session.sync.handler.CryptoSyncHandler
import org.matrix.android.sdk.internal.session.sync.handler.PresenceSyncHandler
import org.matrix.android.sdk.internal.session.sync.handler.SyncResponsePostTreatmentAggregatorHandler
import org.matrix.android.sdk.internal.session.sync.handler.UserAccountDataSyncHandler
import org.matrix.android.sdk.internal.session.sync.handler.room.RoomSyncHandler
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

internal class SyncResponseHandler @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        @SessionId private val sessionId: String,
        private val sessionManager: SessionManager,
        private val sessionListeners: SessionListeners,
        private val roomSyncHandler: RoomSyncHandler,
        private val userAccountDataSyncHandler: UserAccountDataSyncHandler,
        private val cryptoSyncHandler: CryptoSyncHandler,
        private val aggregatorHandler: SyncResponsePostTreatmentAggregatorHandler,
        private val cryptoService: DefaultCryptoService,
        private val tokenStore: SyncTokenStore,
        private val processEventForPushTask: ProcessEventForPushTask,
        private val pushRuleService: PushRuleService,
        private val presenceSyncHandler: PresenceSyncHandler,
        matrixConfiguration: MatrixConfiguration,
) {

    private val relevantPlugins = matrixConfiguration.metricPlugins.filterIsInstance<SyncDurationMetricPlugin>()

    suspend fun handleResponse(
            syncResponse: SyncResponse,
            fromToken: String?,
            reporter: ProgressReporter?
    ) {
        val isInitialSync = fromToken == null
        Timber.v("Start handling sync, is InitialSync: $isInitialSync")

        relevantPlugins.measureMetric {
            startCryptoService(isInitialSync)

            // Handle the to device events before the room ones
            // to ensure to decrypt them properly
            handleToDevice(syncResponse, reporter)

            val aggregator = SyncResponsePostTreatmentAggregator()

            // Prerequisite for thread events handling in RoomSyncHandler
            // Disabled due to the new fallback
            //        if (!lightweightSettingsStorage.areThreadMessagesEnabled()) {
            //            threadsAwarenessHandler.fetchRootThreadEventsIfNeeded(syncResponse)
            //        }

            startMonarchyTransaction(syncResponse, isInitialSync, reporter, aggregator)

            aggregateSyncResponse(aggregator)

            postTreatmentSyncResponse(syncResponse, isInitialSync)

            markCryptoSyncCompleted(syncResponse)

            handlePostSync()

            Timber.v("On sync completed")
        }
    }

    private fun startCryptoService(isInitialSync: Boolean) {
        relevantPlugins.measureSpan("task", "start_crypto_service") {
            measureTimeMillis {
                if (!cryptoService.isStarted()) {
                    Timber.v("Should start cryptoService")
                    cryptoService.start()
                }
                cryptoService.onSyncWillProcess(isInitialSync)
            }.also {
                Timber.v("Finish handling start cryptoService in $it ms")
            }
        }
    }

    private suspend fun handleToDevice(syncResponse: SyncResponse, reporter: ProgressReporter?) {
        relevantPlugins.measureSpan("task", "handle_to_device") {
            measureTimeMillis {
                Timber.v("Handle toDevice")
                reportSubtask(reporter, InitialSyncStep.ImportingAccountCrypto, 100, 0.1f) {
                    if (syncResponse.toDevice != null) {
                        cryptoSyncHandler.handleToDevice(syncResponse.toDevice, reporter)
                    }
                }
            }.also {
                Timber.v("Finish handling toDevice in $it ms")
            }
        }
    }

    private suspend fun startMonarchyTransaction(
            syncResponse: SyncResponse,
            isInitialSync: Boolean,
            reporter: ProgressReporter?,
            aggregator: SyncResponsePostTreatmentAggregator
    ) {
        // Start one big transaction
        relevantPlugins.measureSpan("task", "monarchy_transaction") {
            monarchy.awaitTransaction { realm ->
                // IMPORTANT nothing should be suspend here as we are accessing the realm instance (thread local)
                handleRooms(reporter, syncResponse, realm, isInitialSync, aggregator)
                handleAccountData(reporter, realm, syncResponse)
                handlePresence(realm, syncResponse)

                tokenStore.saveToken(realm, syncResponse.nextBatch)
            }
        }
    }

    private fun handleRooms(
            reporter: ProgressReporter?,
            syncResponse: SyncResponse,
            realm: Realm,
            isInitialSync: Boolean,
            aggregator: SyncResponsePostTreatmentAggregator
    ) {
        relevantPlugins.measureSpan("task", "handle_rooms") {
            measureTimeMillis {
                Timber.v("Handle rooms")
                reportSubtask(reporter, InitialSyncStep.ImportingAccountRoom, 1, 0.8f) {
                    if (syncResponse.rooms != null) {
                        roomSyncHandler.handle(realm, syncResponse.rooms, isInitialSync, aggregator, reporter)
                    }
                }
            }.also {
                Timber.v("Finish handling rooms in $it ms")
            }
        }
    }

    private fun handleAccountData(reporter: ProgressReporter?, realm: Realm, syncResponse: SyncResponse) {
        relevantPlugins.measureSpan("task", "handle_account_data") {
            measureTimeMillis {
                reportSubtask(reporter, InitialSyncStep.ImportingAccountData, 1, 0.1f) {
                    Timber.v("Handle accountData")
                    userAccountDataSyncHandler.handle(realm, syncResponse.accountData)
                }
            }.also {
                Timber.v("Finish handling accountData in $it ms")
            }
        }
    }

    private fun handlePresence(realm: Realm, syncResponse: SyncResponse) {
        relevantPlugins.measureSpan("task", "handle_presence") {
            measureTimeMillis {
                Timber.v("Handle Presence")
                presenceSyncHandler.handle(realm, syncResponse.presence)
            }.also {
                Timber.v("Finish handling Presence in $it ms")
            }
        }
    }

    private suspend fun aggregateSyncResponse(aggregator: SyncResponsePostTreatmentAggregator) {
        relevantPlugins.measureSpan("task", "aggregator_management") {
            // Everything else we need to do outside the transaction
            measureTimeMillis {
                aggregatorHandler.handle(aggregator)
            }.also {
                Timber.v("Aggregator management took $it ms")
            }
        }
    }

    private suspend fun postTreatmentSyncResponse(syncResponse: SyncResponse, isInitialSync: Boolean) {
        relevantPlugins.measureSpan("task", "sync_response_post_treatment") {
            measureTimeMillis {
                syncResponse.rooms?.let {
                    checkPushRules(it, isInitialSync)
                    userAccountDataSyncHandler.synchronizeWithServerIfNeeded(it.invite)
                    dispatchInvitedRoom(it)
                }
            }.also {
                Timber.v("SyncResponse.rooms post treatment took $it ms")
            }
        }
    }

    private fun markCryptoSyncCompleted(syncResponse: SyncResponse) {
        relevantPlugins.measureSpan("task", "crypto_sync_handler_onSyncCompleted") {
            measureTimeMillis {
                cryptoSyncHandler.onSyncCompleted(syncResponse)
            }.also {
                Timber.v("cryptoSyncHandler.onSyncCompleted took $it ms")
            }
        }
    }

    private fun handlePostSync() {
        monarchy.writeAsync {
            roomSyncHandler.postSyncSpaceHierarchyHandle(it)
        }
    }

    private fun dispatchInvitedRoom(roomsSyncResponse: RoomsSyncResponse) {
        val session = sessionManager.getSessionComponent(sessionId)?.session()
        roomsSyncResponse.invite.keys.forEach { roomId ->
            session.dispatchTo(sessionListeners) { session, listener ->
                listener.onNewInvitedRoom(session, roomId)
            }
        }
    }

    private suspend fun checkPushRules(roomsSyncResponse: RoomsSyncResponse, isInitialSync: Boolean) {
        Timber.v("[PushRules] --> checkPushRules")
        if (isInitialSync) {
            Timber.v("[PushRules] <-- No push rule check on initial sync")
            return
        } // nothing on initial sync

        val rules = pushRuleService.getPushRules(RuleScope.GLOBAL).getAllRules()
        processEventForPushTask.execute(ProcessEventForPushTask.Params(roomsSyncResponse, rules))
        Timber.v("[PushRules] <-- Push task scheduled")
    }
}
