/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.internal.crypto.keysbackup.RustKeyBackupService
import org.matrix.android.sdk.internal.crypto.network.RequestSender
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import timber.log.Timber
import uniffi.olm.Request
import uniffi.olm.RequestType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private val loggerTag = LoggerTag("PrepareToEncryptUseCase", LoggerTag.CRYPTO)

@SessionScope
internal class PrepareToEncryptUseCase @Inject constructor(
        private val olmMachine: OlmMachine,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoStore: IMXCryptoStore,
        private val getRoomUserIds: GetRoomUserIdsUseCase,
        private val requestSender: RequestSender,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val keysBackupService: RustKeyBackupService
) {

    private val keyClaimLock: Mutex = Mutex()
    private val roomKeyShareLocks: ConcurrentHashMap<String, Mutex> = ConcurrentHashMap()

    suspend operator fun invoke(roomId: String, ensureAllMembersAreLoaded: Boolean) {
        withContext(coroutineDispatchers.crypto) {
            Timber.tag(loggerTag.value).d("prepareToEncrypt() roomId:$roomId Check room members up to date")
            // Ensure to load all room members
            if (ensureAllMembersAreLoaded) {
                try {
                    loadRoomMembersTask.execute(LoadRoomMembersTask.Params(roomId))
                } catch (failure: Throwable) {
                    Timber.tag(loggerTag.value).e("prepareToEncrypt() : Failed to load room members")
                    throw failure
                }
            }
            val userIds = getRoomUserIds(roomId)
            val algorithm = getEncryptionAlgorithm(roomId)
            if (algorithm == null) {
                val reason = String.format(MXCryptoError.UNABLE_TO_ENCRYPT_REASON, MXCryptoError.NO_MORE_ALGORITHM_REASON)
                Timber.tag(loggerTag.value).e("prepareToEncrypt() : $reason")
                throw IllegalArgumentException("Missing algorithm")
            }
            preshareRoomKey(roomId, userIds)
        }
    }

    private fun getEncryptionAlgorithm(roomId: String): String? {
        return cryptoStore.getRoomAlgorithm(roomId)
    }

    private suspend fun preshareRoomKey(roomId: String, roomMembers: List<String>) {
        claimMissingKeys(roomMembers)
        val keyShareLock = roomKeyShareLocks.getOrPut(roomId) { Mutex() }
        var sharedKey = false
        keyShareLock.withLock {
            coroutineScope {
                olmMachine.shareRoomKey(roomId, roomMembers).map {
                    when (it) {
                        is Request.ToDevice -> {
                            sharedKey = true
                            async {
                                sendToDevice(olmMachine, it)
                            }
                        }
                        else                -> {
                            // This request can only be a to-device request but
                            // we need to handle all our cases and put this
                            // async block for our joinAll to work.
                            async {}
                        }
                    }
                }.joinAll()
            }
        }

        // If we sent out a room key over to-device messages it's likely that we created a new one
        // Try to back the key up
        if (sharedKey) {
            keysBackupService.maybeBackupKeys()
        }
    }

    private suspend fun claimMissingKeys(roomMembers: List<String>) = keyClaimLock.withLock {
        val request = olmMachine.getMissingSessions(roomMembers)
        // This request can only be a keys claim request.
        when (request) {
            is Request.KeysClaim -> {
                claimKeys(request)
            }
            else                 -> {
            }
        }
    }

    private suspend fun sendToDevice(olmMachine: OlmMachine, request: Request.ToDevice) {
        try {
            requestSender.sendToDevice(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.TO_DEVICE, "{}")
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## CRYPTO sendToDevice(): error")
        }
    }

    private suspend fun claimKeys(request: Request.KeysClaim) {
        try {
            val response = requestSender.claimKeys(request)
            olmMachine.markRequestAsSent(request.requestId, RequestType.KEYS_CLAIM, response)
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).e(throwable, "## CRYPTO claimKeys(): error")
        }
    }
}
