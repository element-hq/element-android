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

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.internal.di.UserId
import javax.inject.Inject

internal class ComputeShieldForGroupUseCase @Inject constructor(
        @UserId private val myUserId: String
) {

    suspend operator fun invoke(olmMachine: OlmMachine, userIds: List<String>): RoomEncryptionTrustLevel {
        val myIdentity = olmMachine.getIdentity(myUserId)
        val allTrustedUserIds = userIds
                .filter { userId ->
                    val identity = olmMachine.getIdentity(userId)?.toMxCrossSigningInfo()
                    identity?.isTrusted() == true ||
                            // Always take into account users that was previously verified but are not anymore
                            identity?.wasTrustedOnce == true
                }

        return if (allTrustedUserIds.isEmpty()) {
            RoomEncryptionTrustLevel.Default
        } else {
            // If one of the verified user as an untrusted device -> warning
            // If all devices of all verified users are trusted -> green
            // else -> black
            allTrustedUserIds
                    .map { userId ->
                        olmMachine.getUserDevices(userId)
                    }
                    .flatten()
                    .let { allDevices ->
                        if (myIdentity != null) {
                            allDevices.any { !it.toCryptoDeviceInfo().trustLevel?.crossSigningVerified.orFalse() }
                        } else {
                            // TODO check that if myIdentity is null ean
                            // Legacy method
                            allDevices.any { !it.toCryptoDeviceInfo().isVerified }
                        }
                    }
                    .let { hasWarning ->
                        if (hasWarning) {
                            RoomEncryptionTrustLevel.Warning
                        } else {
                            if (userIds.size == allTrustedUserIds.size) {
                                // all users are trusted and all devices are verified
                                RoomEncryptionTrustLevel.Trusted
                            } else {
                                RoomEncryptionTrustLevel.Default
                            }
                        }
                    }
        }
    }
}
