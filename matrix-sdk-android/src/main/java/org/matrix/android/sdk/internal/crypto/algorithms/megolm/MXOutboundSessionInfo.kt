/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber

internal class MXOutboundSessionInfo(
        // The id of the session
        val sessionId: String,
        val sharedWithHelper: SharedWithHelper,
        private val clock: Clock,
        // When the session was created
        private val creationTime: Long = clock.epochMillis(),
        val sharedHistory: Boolean = false
) {

    // Number of times this session has been used
    var useCount: Int = 0

    fun needsRotation(rotationPeriodMsgs: Int, rotationPeriodMs: Int): Boolean {
        var needsRotation = false
        val sessionLifetime = clock.epochMillis() - creationTime

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
