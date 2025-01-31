/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import java.io.Serializable

internal data class MXOlmSessionResult(
        /**
         * the device.
         */
        val deviceInfo: CryptoDeviceInfo,
        /**
         * Base64 olm session id.
         * null if no session could be established.
         */
        var sessionId: String?
) : Serializable
