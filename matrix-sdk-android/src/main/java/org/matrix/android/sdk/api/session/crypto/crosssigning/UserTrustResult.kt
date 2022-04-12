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
