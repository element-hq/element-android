/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.actions

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.internal.crypto.*
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import timber.log.Timber

internal class MegolmSessionDataImporter(private val mOlmDevice: MXOlmDevice,
                                         private val roomDecryptorProvider: RoomDecryptorProvider,
                                         private val mOutgoingRoomKeyRequestManager: MXOutgoingRoomKeyRequestManager,
                                         private val mCryptoStore: IMXCryptoStore) {

    /**
     * Import a list of megolm session keys.
     *
     * @param megolmSessionsData megolm sessions.
     * @param backUpKeys         true to back up them to the homeserver.
     * @param progressListener   the progress listener
     * @param callback
     */
    fun handle(megolmSessionsData: List<MegolmSessionData>,
               fromBackup: Boolean,
               progressListener: ProgressListener?,
               callback: MatrixCallback<ImportRoomKeysResult>) {
        val t0 = System.currentTimeMillis()

        val totalNumbersOfKeys = megolmSessionsData.size
        var cpt = 0
        var lastProgress = 0
        var totalNumbersOfImportedKeys = 0

        if (progressListener != null) {
            CryptoAsyncHelper.getUiHandler().post {
                progressListener.onProgress(0, 100)
            }
        }

        val sessions = mOlmDevice.importInboundGroupSessions(megolmSessionsData)

        for (megolmSessionData in megolmSessionsData) {
            cpt++


            val decrypting = roomDecryptorProvider.getOrCreateRoomDecryptor(megolmSessionData.roomId, megolmSessionData.algorithm)

            if (null != decrypting) {
                try {
                    val sessionId = megolmSessionData.sessionId
                    Timber.d("## importRoomKeys retrieve mSenderKey " + megolmSessionData.senderKey + " sessionId " + sessionId)

                    totalNumbersOfImportedKeys++

                    // cancel any outstanding room key requests for this session
                    val roomKeyRequestBody = RoomKeyRequestBody()

                    roomKeyRequestBody.algorithm = megolmSessionData.algorithm
                    roomKeyRequestBody.roomId = megolmSessionData.roomId
                    roomKeyRequestBody.senderKey = megolmSessionData.senderKey
                    roomKeyRequestBody.sessionId = megolmSessionData.sessionId

                    mOutgoingRoomKeyRequestManager.cancelRoomKeyRequest(roomKeyRequestBody)

                    // Have another go at decrypting events sent with this session
                    decrypting.onNewSession(megolmSessionData.senderKey!!, sessionId!!)
                } catch (e: Exception) {
                    Timber.e(e, "## importRoomKeys() : onNewSession failed")
                }
            }

            if (progressListener != null) {
                CryptoAsyncHelper.getUiHandler().post {
                    val progress = 100 * cpt / totalNumbersOfKeys

                    if (lastProgress != progress) {
                        lastProgress = progress

                        progressListener.onProgress(progress, 100)
                    }
                }
            }
        }

        // Do not back up the key if it comes from a backup recovery
        if (fromBackup) {
            mCryptoStore.markBackupDoneForInboundGroupSessions(sessions)
        }

        val t1 = System.currentTimeMillis()

        Timber.d("## importMegolmSessionsData : sessions import " + (t1 - t0) + " ms (" + megolmSessionsData.size + " sessions)")

        val finalTotalNumbersOfImportedKeys = totalNumbersOfImportedKeys

        CryptoAsyncHelper.getUiHandler().post {
            callback.onSuccess(ImportRoomKeysResult(totalNumbersOfKeys, finalTotalNumbersOfImportedKeys))
        }
    }
}