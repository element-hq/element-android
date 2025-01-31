/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api

/**
 * This object define some global constants regarding the Matrix specification.
 */
object MatrixConstants {
    /**
     * Max length for an alias. Room aliases MUST NOT exceed 255 bytes (including the # sigil and the domain).
     * See [maxAliasLocalPartLength]
     * Ref. https://matrix.org/docs/spec/appendices#room-aliases
     */
    const val ALIAS_MAX_LENGTH = 255

    fun maxAliasLocalPartLength(domain: String): Int {
        return (ALIAS_MAX_LENGTH - 1 /* # sigil */ - 1 /* ':' */ - domain.length)
                .coerceAtLeast(0)
    }
}
