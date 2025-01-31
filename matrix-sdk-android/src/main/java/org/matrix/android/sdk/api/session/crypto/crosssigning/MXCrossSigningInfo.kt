/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.crosssigning

data class MXCrossSigningInfo(
        val userId: String,
        val crossSigningKeys: List<CryptoCrossSigningKey>
) {

    fun isTrusted(): Boolean = masterKey()?.trustLevel?.isVerified() == true &&
            selfSigningKey()?.trustLevel?.isVerified() == true

    fun masterKey(): CryptoCrossSigningKey? = crossSigningKeys
            .firstOrNull { it.usages?.contains(KeyUsage.MASTER.value) == true }

    fun userKey(): CryptoCrossSigningKey? = crossSigningKeys
            .firstOrNull { it.usages?.contains(KeyUsage.USER_SIGNING.value) == true }

    fun selfSigningKey(): CryptoCrossSigningKey? = crossSigningKeys
            .firstOrNull { it.usages?.contains(KeyUsage.SELF_SIGNING.value) == true }
}
