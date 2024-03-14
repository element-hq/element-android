/*
 * Copyright (c) 2024 New Vector Ltd
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

package im.vector.app.features.analytics

import im.vector.app.features.analytics.plan.Error
import org.matrix.android.sdk.api.session.crypto.MXCryptoError

data class DecryptionFailure(
        val timeStamp: Long,
        val roomId: String,
        val failedEventId: String,
        val error: MXCryptoError,
        val wasVisibleOnScreen: Boolean,
        val ownIdentityTrustedAtTimeOfDecryptionFailure: Boolean,
        // If this is set, it means that the event was decrypted but late
        val timeToDecryptMillis: Long? = null,
        val isMatrixDotOrg: Boolean,
        val isFederated: Boolean? = null,
        val eventLocalAgeAtDecryptionFailure: Long? = null
)

fun DecryptionFailure.toAnalyticsEvent(): Error {
    val errorMsg = (error as? MXCryptoError.Base)?.technicalMessage ?: error.message
    return Error(
            context = "mxc_crypto_error_type|${errorMsg}",
            domain = Error.Domain.E2EE,
            name = this.error.toAnalyticsErrorName(),
            // this is deprecated keep for backward compatibility
            cryptoModule = Error.CryptoModule.Rust
    )
}

fun DecryptionFailure.toCustomProperties(): Map<String, Any> {
    val properties = mutableMapOf<String, Any>()
    if (timeToDecryptMillis != null) {
        properties["timeToDecryptMillis"] = timeToDecryptMillis
    } else {
        properties["timeToDecryptMillis"] = -1
    }
    isFederated?.let {
        properties["isFederated"] = it
    }
    properties["isMatrixDotOrg"] = isMatrixDotOrg
    properties["wasVisibleToUser"] = wasVisibleOnScreen
    properties["userTrustsOwnIdentity"] = ownIdentityTrustedAtTimeOfDecryptionFailure
    eventLocalAgeAtDecryptionFailure?.let {
        properties["eventLocalAgeAtDecryptionFailure"] = it
    }
    return properties
}

private fun MXCryptoError.toAnalyticsErrorName(): Error.Name {
    return if (this is MXCryptoError.Base) {
        when (errorType) {
            MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID,
            MXCryptoError.ErrorType.KEYS_WITHHELD         -> Error.Name.OlmKeysNotSentError
            MXCryptoError.ErrorType.OLM                   -> Error.Name.OlmUnspecifiedError
            MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX -> Error.Name.OlmIndexError
            else                                          -> Error.Name.UnknownError
        }
    } else {
        Error.Name.UnknownError
    }
}
