/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.mapper

import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import javax.inject.Inject

internal class MyDeviceLastSeenInfoEntityMapper @Inject constructor() {

    fun map(entity: MyDeviceLastSeenInfoEntity): DeviceInfo {
        return DeviceInfo(
                deviceId = entity.deviceId,
                lastSeenIp = entity.lastSeenIp,
                lastSeenTs = entity.lastSeenTs,
                displayName = entity.displayName
        )
    }
}
