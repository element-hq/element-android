/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.crypto.crosssigning

sealed class UserTrustResult {
    object Success : UserTrustResult()

    // data class Success(val deviceID: String, val crossSigned: Boolean) : UserTrustResult()
    // data class UnknownDevice(val deviceID: String) : UserTrustResult()
    data class CrossSigningNotConfigured(val userID: String) : UserTrustResult()

    data class UnknownCrossSignatureInfo(val userID: String) : UserTrustResult()
    data class KeysNotTrusted(val key: MXCrossSigningInfo) : UserTrustResult()
    data class KeyNotSigned(val key: CryptoCrossSigningKey) : UserTrustResult()
    data class InvalidSignature(val key: CryptoCrossSigningKey, val signature: String) : UserTrustResult()
}

fun UserTrustResult.isVerified() = this is UserTrustResult.Success
