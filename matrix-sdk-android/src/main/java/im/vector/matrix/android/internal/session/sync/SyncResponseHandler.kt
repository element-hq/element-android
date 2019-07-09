/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync

import arrow.core.Try
import im.vector.matrix.android.R
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.session.DefaultInitialSyncProgressService
import im.vector.matrix.android.internal.session.reportSubtask
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

internal class SyncResponseHandler @Inject constructor(private val roomSyncHandler: RoomSyncHandler,
                                                       private val userAccountDataSyncHandler: UserAccountDataSyncHandler,
                                                       private val groupSyncHandler: GroupSyncHandler,
                                                       private val cryptoSyncHandler: CryptoSyncHandler,
                                                       private val cryptoManager: CryptoManager,
                                                       private val initialSyncProgressService: DefaultInitialSyncProgressService) {

    fun handleResponse(syncResponse: SyncResponse, fromToken: String?, isCatchingUp: Boolean): Try<SyncResponse> {
        return Try {
            val isInitialSync = fromToken == null
            Timber.v("Start handling sync, is InitialSync: $isInitialSync")
            val reporter = initialSyncProgressService.takeIf { isInitialSync }

            if (!cryptoManager.isStarted()) {
                Timber.v("Should start cryptoManager")
                cryptoManager.start(isInitialSync)
            }
            val measure = measureTimeMillis {
                // Handle the to device events before the room ones
                // to ensure to decrypt them properly
                Timber.v("Handle toDevice")
                reportSubtask(reporter, R.string.initial_sync_start_importing_account_crypto, 100, 0.2f) {
                    if (syncResponse.toDevice != null) {
                        cryptoSyncHandler.handleToDevice(syncResponse.toDevice, reporter)
                    }
                }
                Timber.v("Handle rooms")

                reportSubtask(reporter, R.string.initial_sync_start_importing_account_rooms, 100, 0.5f) {
                    if (syncResponse.rooms != null) {
                        roomSyncHandler.handle(syncResponse.rooms, reporter)
                    }
                }

                reportSubtask(reporter, R.string.initial_sync_start_importing_account_groups, 100, 0.2f) {
                    Timber.v("Handle groups")
                    if (syncResponse.groups != null) {
                        groupSyncHandler.handle(syncResponse.groups, reporter)
                    }
                }

                reportSubtask(reporter, R.string.initial_sync_start_importing_account_data, 100, 0.1f) {
                    Timber.v("Handle accountData")
                    if (syncResponse.accountData != null) {
                        userAccountDataSyncHandler.handle(syncResponse.accountData)
                    }
                }

                Timber.v("On sync completed")
                cryptoSyncHandler.onSyncCompleted(syncResponse)

            }
            Timber.v("Finish handling sync in $measure ms")
            syncResponse
        }
    }

}