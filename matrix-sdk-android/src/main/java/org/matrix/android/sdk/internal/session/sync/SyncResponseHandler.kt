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
import org.matrix.android.sdk.api.extensions.measureSpan
import org.matrix.android.sdk.api.extensions.measureSpannableMetric
import org.matrix.android.sdk.api.metrics.SpannableMetricPlugin
import org.matrix.android.sdk.api.metrics.SyncDurationMetricPlugin
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.pushrules.PushRuleService
import org.matrix.android.sdk.api.session.pushrules.RuleScope
import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import org.matrix.android.sdk.api.session.sync.model.RoomsSyncResponse
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.crypto.store.db.CryptoStoreAggregator
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.SessionListeners
import org.matrix.android.sdk.internal.session.dispatchTo
import org.matrix.android.sdk.internal.session.pushrules.ProcessEventForPushTask
import org.matrix.android.sdk.internal.session.sync.handler.PresenceSyncHandler
import org.matrix.android.sdk.internal.session.sync.handler.SyncResponsePostTreatmentAggregatorHandler
import org.matrix.android.sdk.internal.session.sync.handler.UserAccountDataSyncHandler
import org.matrix.android.sdk.internal.session.sync.handler.room.RoomSyncHandler
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
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
        private val aggregatorHandler: SyncResponsePostTreatmentAggregatorHandler,
        private val cryptoService: CryptoService,
        private val tokenStore: SyncTokenStore,
        private val processEventForPushTask: ProcessEventForPushTask,
        private val pushRuleService: PushRuleService,
        private val presenceSyncHandler: PresenceSyncHandler,
        private val clock: Clock,
        matrixConfiguration: MatrixConfiguration,
) {

    private val relevantPlugins = matrixConfiguration.metricPlugins.filterIsInstance<SyncDurationMetricPlugin>()

    suspend fun handleResponse(
            syncResponse: SyncResponse,
            fromToken: String?,
            afterPause: Boolean,
            reporter: ProgressReporter?
    ) {
        val isInitialSync = fromToken == null

        val aggregator = SyncResponsePostTreatmentAggregator()

        relevantPlugins.filter { it.shouldReport(isInitialSync, afterPause) }.measureSpannableMetric {
            startCryptoService(isInitialSync)

            // Handle the to device events before the room ones
            // to ensure to decrypt them properly
            handleToDevice(syncResponse)

            val syncLocalTimestampMillis = clock.epochMillis()

            // pass live state/crypto related event to crypto

            measureSpan("task", "crypto_session_event_handling") {
                syncResponse.rooms?.invite?.entries?.map { (roomId, roomSync) ->
                    roomSync.inviteState
                            ?.events
                            ?.filter { it.isStateEvent() }
                            ?.forEach {
                                cryptoService.onStateEvent(roomId, it, aggregator.cryptoStoreAggregator)
                            }
                }

                syncResponse.rooms?.join?.entries?.map { (roomId, roomSync) ->
                    roomSync.state
                            ?.events
                            ?.filter { it.isStateEvent() }
                            ?.forEach {
                                cryptoService.onStateEvent(roomId, it, aggregator.cryptoStoreAggregator)
                            }

                    roomSync.timeline?.events?.forEach {
                        if (it.isEncrypted() && !isInitialSync) {
                            decryptIfNeeded(it, roomId)
                        }
                        it.ageLocalTs = syncLocalTimestampMillis - (it.unsignedData?.age ?: 0)
                        cryptoService.onLiveEvent(roomId, it, isInitialSync, aggregator.cryptoStoreAggregator)
                    }
                }
            }

            // Prerequisite for thread events handling in RoomSyncHandler
            // Disabled due to the new fallback
            //        if (!lightweightSettingsStorage.areThreadMessagesEnabled()) {
            //            threadsAwarenessHandler.fetchRootThreadEventsIfNeeded(syncResponse)
            //        }

            startMonarchyTransaction(syncResponse, isInitialSync, reporter, aggregator)

            aggregateSyncResponse(aggregator)

            postTreatmentSyncResponse(syncResponse, isInitialSync)

            markCryptoSyncCompleted(syncResponse, aggregator.cryptoStoreAggregator)

            handlePostSync()

            Timber.v("On sync completed")
        }
    }

    private suspend fun decryptIfNeeded(event: Event, roomId: String) {
        try {
            val timelineId = generateTimelineId(roomId)
            // Event from sync does not have roomId, so add it to the event first
            val result = cryptoService.decryptEvent(event.copy(roomId = roomId), timelineId)
            event.mxDecryptionResult = OlmDecryptionResult(
                    payload = result.clearEvent,
                    senderKey = result.senderCurve25519Key,
                    keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                    forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                    verificationState = result.messageVerificationState
            )
        } catch (e: MXCryptoError) {
            Timber.v(e, "Failed to decrypt $roomId")
            if (e is MXCryptoError.Base) {
                event.mCryptoError = e.errorType
                event.mCryptoErrorReason = e.technicalMessage.takeIf { it.isNotEmpty() } ?: e.detailedErrorDescription
            }
        }
    }

    private fun generateTimelineId(roomId: String): String {
        return "RoomSyncHandler$roomId"
    }

    private suspend fun List<SpannableMetricPlugin>.startCryptoService(isInitialSync: Boolean) {
        measureSpan("task", "start_crypto_service") {
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

    private suspend fun List<SpannableMetricPlugin>.handleToDevice(syncResponse: SyncResponse) {
        measureSpan("task", "handle_to_device") {
            measureTimeMillis {
                Timber.v("Handle toDevice")
                cryptoService.receiveSyncChanges(
                        syncResponse.toDevice,
                        syncResponse.deviceLists,
                        syncResponse.deviceOneTimeKeysCount,
                        syncResponse.deviceUnusedFallbackKeyTypes,
                        syncResponse.nextBatch
                )
            }.also {
                Timber.v("Finish handling toDevice in $it ms")
            }
        }
    }

    private suspend fun List<SpannableMetricPlugin>.startMonarchyTransaction(
            syncResponse: SyncResponse,
            isInitialSync: Boolean,
            reporter: ProgressReporter?,
            aggregator: SyncResponsePostTreatmentAggregator
    ) {
        // Start one big transaction
        measureSpan("task", "monarchy_transaction") {
            monarchy.awaitTransaction { realm ->
                // IMPORTANT nothing should be suspend here as we are accessing the realm instance (thread local)
                handleRooms(reporter, syncResponse, realm, isInitialSync, aggregator)
                handleAccountData(reporter, realm, syncResponse)
                handlePresence(realm, syncResponse)

                tokenStore.saveToken(realm, syncResponse.nextBatch)
            }
        }
    }

    private fun List<SpannableMetricPlugin>.handleRooms(
            reporter: ProgressReporter?,
            syncResponse: SyncResponse,
            realm: Realm,
            isInitialSync: Boolean,
            aggregator: SyncResponsePostTreatmentAggregator
    ) {
        measureSpan("task", "handle_rooms") {
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

    private fun List<SpannableMetricPlugin>.handleAccountData(reporter: ProgressReporter?, realm: Realm, syncResponse: SyncResponse) {
        measureSpan("task", "handle_account_data") {
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

    private fun List<SpannableMetricPlugin>.handlePresence(realm: Realm, syncResponse: SyncResponse) {
        measureSpan("task", "handle_presence") {
            measureTimeMillis {
                Timber.v("Handle Presence")
                presenceSyncHandler.handle(realm, syncResponse.presence)
            }.also {
                Timber.v("Finish handling Presence in $it ms")
            }
        }
    }

    private suspend fun List<SpannableMetricPlugin>.aggregateSyncResponse(aggregator: SyncResponsePostTreatmentAggregator) {
        measureSpan("task", "aggregator_management") {
            // Everything else we need to do outside the transaction
            measureTimeMillis {
                aggregatorHandler.handle(aggregator)
            }.also {
                Timber.v("Aggregator management took $it ms")
            }
        }
    }

    private suspend fun List<SpannableMetricPlugin>.postTreatmentSyncResponse(syncResponse: SyncResponse, isInitialSync: Boolean) {
        measureSpan("task", "sync_response_post_treatment") {
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

    private suspend fun List<SpannableMetricPlugin>.markCryptoSyncCompleted(syncResponse: SyncResponse, cryptoStoreAggregator: CryptoStoreAggregator) {
        measureSpan("task", "crypto_sync_handler_onSyncCompleted") {
            measureTimeMillis {
                cryptoService.onSyncCompleted(syncResponse, cryptoStoreAggregator)
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
