/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

fun String.formatRecoveryKey(): String {
    return this.replace(" ", "")
            .chunked(16)
            .joinToString("\n") {
                it
                        .chunked(4)
                        .joinToString(" ")
            }
}
