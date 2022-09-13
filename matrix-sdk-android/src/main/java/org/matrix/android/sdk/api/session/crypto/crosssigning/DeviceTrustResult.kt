/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
