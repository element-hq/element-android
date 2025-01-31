/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.mapper

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity

private const val A_DEVICE_ID = "device-id"
private const val AN_IP_ADDRESS = "ip-address"
private const val A_TIMESTAMP = 123L
private const val A_DISPLAY_NAME = "display-name"

class MyDeviceLastSeenInfoEntityMapperTest {

    private val myDeviceLastSeenInfoEntityMapper = MyDeviceLastSeenInfoEntityMapper()

    @Test
    fun `given an entity when mapping to model then all fields are correctly mapped`() {
        val entity = MyDeviceLastSeenInfoEntity(
                deviceId = A_DEVICE_ID,
                lastSeenIp = AN_IP_ADDRESS,
                lastSeenTs = A_TIMESTAMP,
                displayName = A_DISPLAY_NAME
        )
        val expectedDeviceInfo = DeviceInfo(
                deviceId = A_DEVICE_ID,
                lastSeenIp = AN_IP_ADDRESS,
                lastSeenTs = A_TIMESTAMP,
                displayName = A_DISPLAY_NAME
        )

        val deviceInfo = myDeviceLastSeenInfoEntityMapper.map(entity)

        deviceInfo shouldBeEqualTo expectedDeviceInfo
    }
}
