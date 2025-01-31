/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.util

import android.util.Base64

fun ByteArray.toBase64NoPadding(): String {
    return Base64.encodeToString(this, Base64.NO_PADDING or Base64.NO_WRAP)
}

fun String.fromBase64(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}
