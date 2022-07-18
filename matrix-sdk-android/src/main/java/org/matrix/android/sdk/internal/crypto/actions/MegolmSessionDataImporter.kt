/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.actions

import androidx.annotation.WorkerThread
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.MegolmSessionData
import org.matrix.android.sdk.internal.crypto.OutgoingKeyRequestManager
import org.matrix.android.sdk.internal.crypto.RoomDecryptorProvider
import org.matrix.android.sdk.internal.crypto.algorithms.megolm.MXMegolmDecryption
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("MegolmSessionDataImporter", LoggerTag.CRYPTO)

internal class MegolmSessionDataImporter @Inject constructor(
        private val olmDevice: MXOlmDevice,
        private val roomDecryptorProvider: RoomDecryptorProvider,
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
        private val cryptoStore: IMXCryptoStore,
        private val clock: Clock,
) {

    /**
     * Import a list of megolm session keys.
     * Must be call on the crypto coroutine thread
     *
     * @param megolmSessionsData megolm sessions.
     * @param fromBackup true if the imported keys are already backed up on the server.
     * @param progressListener the progress listener
     * @return import room keys result
     */
    @WorkerThread
    fun handle(
            megolmSessionsData: List<MegolmSessionData>,
            fromBackup: Boolean,
            progressListener: ProgressListener?
    ): ImportRoomKeysResult {
        val t0 = clock.epochMillis()

        val totalNumbersOfKeys = megolmSessionsData.size
        var lastProgress = 0
        var totalNumbersOfImportedKeys = 0

        progressListener?.onProgress(0, 100)
        val olmInboundGroupSessionWrappers = olmDevice.importInboundGroupSessions(megolmSessionsData)

        megolmSessionsData.forEachIndexed { cpt, megolmSessionData ->
            val decrypting = roomDecryptorProvider.getOrCreateRoomDecryptor(megolmSessionData.roomId, megolmSessionData.algorithm)

            if (null != decrypting) {
                try {
                    val sessionId = megolmSessionData.sessionId
                    Timber.tag(loggerTag.value).v("## importRoomKeys retrieve senderKey ${megolmSessionData.senderKey} sessionId $sessionId")

                    totalNumbersOfImportedKeys++

                    // cancel any outstanding room key requests for this session

                    Timber.tag(loggerTag.value).d("Imported megolm session $sessionId from backup=$fromBackup in ${megolmSessionData.roomId}")
                    outgoingKeyRequestManager.postCancelRequestForSessionIfNeeded(
                            megolmSessionData.sessionId ?: "",
                            megolmSessionData.roomId ?: "",
                            megolmSessionData.senderKey ?: "",
                            tryOrNull {
                                olmInboundGroupSessionWrappers
                                        .firstOrNull { it.session.sessionIdentifier() == megolmSessionData.sessionId }
                                        ?.session?.firstKnownIndex
                                        ?.toInt()
                            } ?: 0
                    )

                    // Have another go at decrypting events sent with this session
                    when (decrypting) {
                        is MXMegolmDecryption -> {
                            decrypting.onNewSession(megolmSessionData.roomId, megolmSessionData.senderKey!!, sessionId!!)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(loggerTag.value).e(e, "## importRoomKeys() : onNewSession failed")
                }
            }

            if (progressListener != null) {
                val progress = 100 * (cpt + 1) / totalNumbersOfKeys

                if (lastProgress != progress) {
                    lastProgress = progress

                    progressListener.onProgress(progress, 100)
                }
            }
        }

        // Do not back up the key if it comes from a backup recovery
        if (fromBackup) {
            cryptoStore.markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers)
        }

        val t1 = clock.epochMillis()

        Timber.tag(loggerTag.value).v("## importMegolmSessionsData : sessions import " + (t1 - t0) + " ms (" + megolmSessionsData.size + " sessions)")

        return ImportRoomKeysResult(totalNumbersOfKeys, totalNumbersOfImportedKeys)
    }
}
