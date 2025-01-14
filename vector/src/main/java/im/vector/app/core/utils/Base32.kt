/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.utils

import org.apache.commons.codec.binary.Base32

fun String.toBase32String(padding: Boolean = true): String {
    val base32 = Base32().encodeAsString(toByteArray())
    return if (padding) {
        base32
    } else {
        base32.trimEnd('=')
    }
}
