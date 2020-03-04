/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.crosssigning

import im.vector.matrix.android.api.crypto.RoomEncryptionTrustLevel
import im.vector.matrix.android.api.extensions.orFalse
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal interface ComputeTrustTask : Task<ComputeTrustTask.Params, RoomEncryptionTrustLevel> {
    data class Params(
            val userIds: List<String>
    )
}

internal class DefaultComputeTrustTask @Inject constructor(
        private val cryptoStore: IMXCryptoStore,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) : ComputeTrustTask {

    override suspend fun execute(params: ComputeTrustTask.Params): RoomEncryptionTrustLevel = withContext(coroutineDispatchers.crypto) {
        val allTrustedUserIds = params.userIds
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
                            if (params.userIds.size == allTrustedUserIds.size) {
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
