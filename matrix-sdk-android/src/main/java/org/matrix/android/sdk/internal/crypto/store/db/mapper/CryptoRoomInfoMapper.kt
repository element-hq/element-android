/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.crypto.MEGOLM_DEFAULT_ROTATION_MSGS
import org.matrix.android.sdk.api.crypto.MEGOLM_DEFAULT_ROTATION_PERIOD_MS
import org.matrix.android.sdk.api.session.crypto.model.CryptoRoomInfo
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity

internal object CryptoRoomInfoMapper {

    fun map(entity: CryptoRoomEntity): CryptoRoomInfo? {
        val algorithm = entity.algorithm ?: return null
        return CryptoRoomInfo(
                algorithm = algorithm,
                shouldEncryptForInvitedMembers = entity.shouldEncryptForInvitedMembers ?: false,
                blacklistUnverifiedDevices = entity.blacklistUnverifiedDevices,
                shouldShareHistory = entity.shouldShareHistory,
                wasEncryptedOnce = entity.wasEncryptedOnce ?: false,
                rotationPeriodMsgs = entity.rotationPeriodMsgs ?: MEGOLM_DEFAULT_ROTATION_MSGS,
                rotationPeriodMs = entity.rotationPeriodMs ?: MEGOLM_DEFAULT_ROTATION_PERIOD_MS
        )
    }
}
