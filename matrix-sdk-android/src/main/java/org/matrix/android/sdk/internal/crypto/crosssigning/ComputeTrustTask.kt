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
package org.matrix.android.sdk.internal.crypto.crosssigning

import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ComputeTrustTask : Task<ComputeTrustTask.Params, RoomEncryptionTrustLevel> {
    data class Params(
            val activeMemberUserIds: List<String>,
            val isDirectRoom: Boolean
    )
}

internal class DefaultComputeTrustTask @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
        @UserId private val userId: String,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) : ComputeTrustTask {

    override suspend fun execute(params: ComputeTrustTask.Params): RoomEncryptionTrustLevel = withContext(coroutineDispatchers.crypto) {
        // The set of “all users” depends on the type of room:
        // For regular / topic rooms, all users including yourself, are considered when decorating a room
        // For 1:1 and group DM rooms, all other users (i.e. excluding yourself) are considered when decorating a room
        val listToCheck = if (params.isDirectRoom) {
            params.activeMemberUserIds.filter { it != userId }
        } else {
            params.activeMemberUserIds
        }

        val allTrustedUserIds = listToCheck
                .filter { userId -> getUserCrossSigningKeys(userId)?.isTrusted() == true }

        if (allTrustedUserIds.isEmpty()) {
            RoomEncryptionTrustLevel.Default
        } else {
            // If one of the verified user as an untrusted device -> warning
            // If all devices of all verified users are trusted -> green
            // else -> black
            allTrustedUserIds
                    .mapNotNull { cryptoStore.getUserDeviceList(it) }
                    .flatten()
                    .let { allDevices ->
                        if (getMyCrossSigningKeys() != null) {
                            allDevices.any { !it.trustLevel?.crossSigningVerified.orFalse() }
                        } else {
                            // Legacy method
                            allDevices.any { !it.isVerified }
                        }
                    }
                    .let { hasWarning ->
                        if (hasWarning) {
                            RoomEncryptionTrustLevel.Warning
                        } else {
                            if (listToCheck.size == allTrustedUserIds.size) {
                                // all users are trusted and all devices are verified
                                RoomEncryptionTrustLevel.Trusted
                            } else {
                                RoomEncryptionTrustLevel.Default
                            }
                        }
                    }
        }
    }

    private fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo? {
        return cryptoStore.getCrossSigningInfo(otherUserId)
    }

    private fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return cryptoStore.getMyCrossSigningInfo()
    }
}
