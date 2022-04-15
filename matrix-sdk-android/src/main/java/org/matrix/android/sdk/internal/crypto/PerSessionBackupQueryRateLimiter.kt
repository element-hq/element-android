/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import dagger.Lazy
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.SavedKeyBackupKeyInfo
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import timber.log.Timber
import javax.inject.Inject

// I keep the same name as OutgoingGossipingRequestManager to ease filtering of logs
private val loggerTag = LoggerTag("OutgoingGossipingRequestManager", LoggerTag.CRYPTO)

/**
 * Used to try to get the key for UISI messages before sending room key request.
 * We are adding some rate limiting to avoid querying too much for a key not in backup.
 * Nonetheless the backup can be updated so we might want to retry from time to time.
 */
internal class PerSessionBackupQueryRateLimiter @Inject constructor(
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val keysBackupService: Lazy<DefaultKeysBackupService>,
        private val cryptoStore: IMXCryptoStore
) {

    companion object {
        val MIN_TRY_BACKUP_PERIOD_MILLIS = 60 * 60_000 // 1 hour
    }

    data class Info(
            val megolmSessionId: String,
            val roomId: String
    )

    data class LastTry(
            val backupVersion: String,
            val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Remember what we already tried (a key not in backup or some server issue)
     * We might want to retry from time to time as the backup could have been updated
     */
    private val lastFailureMap = mutableMapOf<Info, LastTry>()

    private var backupVersion: KeysVersionResult? = null
    private var savedKeyBackupKeyInfo: SavedKeyBackupKeyInfo? = null
    var backupWasCheckedFromServer: Boolean = false
    var now = System.currentTimeMillis()

    fun refreshBackupInfoIfNeeded(force: Boolean = false) {
        if (backupWasCheckedFromServer && !force) return
        Timber.tag(loggerTag.value).v("Checking if can access a backup")
        backupWasCheckedFromServer = true
        val knownBackupSecret = cryptoStore.getKeyBackupRecoveryKeyInfo()
                ?: return Unit.also {
                    Timber.tag(loggerTag.value).v("We don't have the backup secret!")
                }
        this.backupVersion = keysBackupService.get().keysBackupVersion
        this.savedKeyBackupKeyInfo = knownBackupSecret
    }

    suspend fun tryFromBackupIfPossible(sessionId: String, roomId: String): Boolean {
        Timber.tag(loggerTag.value).v("tryFromBackupIfPossible for session:$sessionId in $roomId")
        refreshBackupInfoIfNeeded()
        val currentVersion = backupVersion
        if (savedKeyBackupKeyInfo?.version == null ||
                currentVersion == null ||
                currentVersion.version != savedKeyBackupKeyInfo?.version) {
            // We can't access the backup
            Timber.tag(loggerTag.value).v("Can't get backup version info")
            return false
        }
        val cacheKey = Info(sessionId, roomId)
        val lastTry = lastFailureMap[cacheKey]
        val shouldQuery =
                lastTry == null ||
                        lastTry.backupVersion != currentVersion.version ||
                        (now - lastTry.timestamp) > MIN_TRY_BACKUP_PERIOD_MILLIS

        if (!shouldQuery) return false

        val successfullyImported = withContext(coroutineDispatchers.io) {
            try {
                awaitCallback<ImportRoomKeysResult> {
                    keysBackupService.get().restoreKeysWithRecoveryKey(
                            currentVersion,
                            savedKeyBackupKeyInfo?.recoveryKey ?: "",
                            roomId,
                            sessionId,
                            null,
                            it
                    )
                }.successfullyNumberOfImportedKeys
            } catch (failure: Throwable) {
                // Fail silently
                Timber.tag(loggerTag.value).v("getFromBackup failed ${failure.localizedMessage}")
                0
            }
        }
        if (successfullyImported == 1) {
            Timber.tag(loggerTag.value).v("Found key in backup session:$sessionId in $roomId")
            lastFailureMap.remove(cacheKey)
            return true
        } else {
            Timber.tag(loggerTag.value).v("Failed to find key in backup session:$sessionId in $roomId")
            lastFailureMap[cacheKey] = LastTry(currentVersion.version)
            return false
        }
    }
}
