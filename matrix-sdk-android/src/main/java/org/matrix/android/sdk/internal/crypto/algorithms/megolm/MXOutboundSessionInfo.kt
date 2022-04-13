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

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import timber.log.Timber

internal class MXOutboundSessionInfo(
        // The id of the session
        val sessionId: String,
        val sharedWithHelper: SharedWithHelper,
        // When the session was created
        private val creationTime: Long = System.currentTimeMillis()) {

    // Number of times this session has been used
    var useCount: Int = 0

    fun needsRotation(rotationPeriodMsgs: Int, rotationPeriodMs: Int): Boolean {
        var needsRotation = false
        val sessionLifetime = System.currentTimeMillis() - creationTime

        if (useCount >= rotationPeriodMsgs || sessionLifetime >= rotationPeriodMs) {
            Timber.v("## needsRotation() : Rotating megolm session after $useCount, ${sessionLifetime}ms")
            needsRotation = true
        }

        return needsRotation
    }

    /**
     * Determine if this session has been shared with devices which it shouldn't have been.
     *
     * @param devicesInRoom the devices map
     * @return true if we have shared the session with devices which aren't in devicesInRoom.
     */
    fun sharedWithTooManyDevices(devicesInRoom: MXUsersDevicesMap<CryptoDeviceInfo>): Boolean {
        val sharedWithDevices = sharedWithHelper.sharedWithDevices()
        val userIds = sharedWithDevices.userIds

        for (userId in userIds) {
            if (null == devicesInRoom.getUserDeviceIds(userId)) {
                Timber.v("## sharedWithTooManyDevices() : Starting new session because we shared with $userId")
                return true
            }

            val deviceIds = sharedWithDevices.getUserDeviceIds(userId)

            for (deviceId in deviceIds!!) {
                if (null == devicesInRoom.getObject(userId, deviceId)) {
                    Timber.v("## sharedWithTooManyDevices() : Starting new session because we shared with $userId:$deviceId")
                    return true
                }
            }
        }

        return false
    }
}
