/*
 * Copyright 2015 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.crypto.algorithms.megolm

import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import timber.log.Timber

internal class MXOutboundSessionInfo(
        // The id of the session
        val mSessionId: String) {
    // When the session was created
    private val mCreationTime = System.currentTimeMillis()

    // Number of times this session has been used
    var mUseCount: Int = 0

    // Devices with which we have shared the session key
    // userId -> {deviceId -> msgindex}
    val mSharedWithDevices: MXUsersDevicesMap<Int> = MXUsersDevicesMap()

    fun needsRotation(rotationPeriodMsgs: Int, rotationPeriodMs: Int): Boolean {
        var needsRotation = false
        val sessionLifetime = System.currentTimeMillis() - mCreationTime

        if (mUseCount >= rotationPeriodMsgs || sessionLifetime >= rotationPeriodMs) {
            Timber.d("## needsRotation() : Rotating megolm session after " + mUseCount + ", " + sessionLifetime + "ms")
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
    fun sharedWithTooManyDevices(devicesInRoom: MXUsersDevicesMap<MXDeviceInfo>): Boolean {
        val userIds = mSharedWithDevices.userIds

        for (userId in userIds) {
            if (null == devicesInRoom.getUserDeviceIds(userId)) {
                Timber.d("## sharedWithTooManyDevices() : Starting new session because we shared with $userId")
                return true
            }

            val deviceIds = mSharedWithDevices.getUserDeviceIds(userId)

            for (deviceId in deviceIds!!) {
                if (null == devicesInRoom.getObject(deviceId, userId)) {
                    Timber.d("## sharedWithTooManyDevices() : Starting new session because we shared with $userId:$deviceId")
                    return true
                }
            }
        }

        return false
    }
}
