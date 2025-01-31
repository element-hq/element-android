/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.crosssigning

data class PrivateKeysInfo(
        val master: String? = null,
        val selfSigned: String? = null,
        val user: String? = null
) {
    fun allKnown() = master != null && selfSigned != null && user != null
}
