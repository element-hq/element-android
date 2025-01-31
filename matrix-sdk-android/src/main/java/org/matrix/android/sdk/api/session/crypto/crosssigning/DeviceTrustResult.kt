/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.crypto.crosssigning

sealed class DeviceTrustResult {
    data class Success(val level: DeviceTrustLevel) : DeviceTrustResult()
    data class UnknownDevice(val deviceID: String) : DeviceTrustResult()
    data class CrossSigningNotConfigured(val userID: String) : DeviceTrustResult()
    data class KeysNotTrusted(val key: MXCrossSigningInfo) : DeviceTrustResult()
    data class MissingDeviceSignature(val deviceId: String, val signingKey: String) : DeviceTrustResult()
    data class InvalidDeviceSignature(val deviceId: String, val signingKey: String, val throwable: Throwable?) : DeviceTrustResult()
}

fun DeviceTrustResult.isSuccess() = this is DeviceTrustResult.Success
fun DeviceTrustResult.isCrossSignedVerified() = this is DeviceTrustResult.Success && level.isCrossSigningVerified()
fun DeviceTrustResult.isLocallyVerified() = this is DeviceTrustResult.Success && level.isLocallyVerified() == true
