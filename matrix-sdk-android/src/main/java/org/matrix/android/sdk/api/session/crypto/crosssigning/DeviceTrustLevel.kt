/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.crypto.crosssigning

data class DeviceTrustLevel(
        val crossSigningVerified: Boolean,
        val locallyVerified: Boolean?
) {
    fun isVerified() = crossSigningVerified || locallyVerified == true
    fun isCrossSigningVerified() = crossSigningVerified
    fun isLocallyVerified() = locallyVerified
}
