/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.model

/**
 * Generic crypto info.
 * Can be a device (CryptoDeviceInfo), as well as a CryptoCrossSigningInfo (can be seen as a kind of virtual device)
 */
internal interface CryptoInfo {

    val userId: String

    val keys: Map<String, String>?

    val signatures: Map<String, Map<String, String>>?

    fun signalableJSONDictionary(): Map<String, Any>
}
