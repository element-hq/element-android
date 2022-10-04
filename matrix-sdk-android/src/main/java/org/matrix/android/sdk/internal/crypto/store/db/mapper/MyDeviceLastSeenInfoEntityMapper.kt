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
                displayName = entity.displayName,
                unstableLastSeenUserAgent = entity.lastSeenUserAgent,
        )
    }

    fun map(deviceInfo: DeviceInfo): MyDeviceLastSeenInfoEntity {
        return MyDeviceLastSeenInfoEntity(
                deviceId = deviceInfo.deviceId,
                lastSeenIp = deviceInfo.lastSeenIp,
                lastSeenTs = deviceInfo.lastSeenTs,
                displayName = deviceInfo.displayName,
                lastSeenUserAgent = deviceInfo.getBestLastSeenUserAgent(),
        )
    }
}
