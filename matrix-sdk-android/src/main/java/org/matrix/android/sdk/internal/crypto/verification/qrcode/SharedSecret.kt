/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import org.matrix.android.sdk.api.util.toBase64NoPadding
import java.security.SecureRandom

internal fun generateSharedSecretV2(): String {
    val secureRandom = SecureRandom()

    // 8 bytes long
    val secretBytes = ByteArray(8)
    secureRandom.nextBytes(secretBytes)
    return secretBytes.toBase64NoPadding()
}
