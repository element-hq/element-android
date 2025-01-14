/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo

object CryptoDeviceInfoFixture {

    fun aCryptoDeviceInfo(
            deviceId: String = "",
            userId: String = "",
            algorithms: List<String>? = null,
            keys: Map<String, String>? = null,
            signatures: Map<String, Map<String, String>>? = null,
            unsigned: UnsignedDeviceInfo? = null,
            trustLevel: DeviceTrustLevel? = null,
            isBlocked: Boolean = false,
            firstTimeSeenLocalTs: Long? = null,
    ) = CryptoDeviceInfo(
            deviceId,
            userId,
            algorithms,
            keys,
            signatures,
            unsigned,
            trustLevel,
            isBlocked,
            firstTimeSeenLocalTs,
    )
}
