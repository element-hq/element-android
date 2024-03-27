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
        // If this is set, it means that the event was decrypted but late. Will be -1 if
        // the event was not decrypted after the maximum wait time.
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
            name = this.toAnalyticsErrorName(),
            // this is deprecated keep for backward compatibility
            cryptoModule = Error.CryptoModule.Rust,
            cryptoSDK = Error.CryptoSDK.Rust,
            eventLocalAgeMillis = eventLocalAgeAtDecryptionFailure?.toInt(),
            isFederated = isFederated,
            isMatrixDotOrg = isMatrixDotOrg,
            timeToDecryptMillis = timeToDecryptMillis?.toInt() ?: -1,
            wasVisibleToUser = wasVisibleOnScreen,
            userTrustsOwnIdentity = ownIdentityTrustedAtTimeOfDecryptionFailure,
    )
}

private fun DecryptionFailure.toAnalyticsErrorName(): Error.Name {
    val error = this.error
    val name = if (error is MXCryptoError.Base) {
        when (error.errorType) {
            MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID,
            MXCryptoError.ErrorType.KEYS_WITHHELD         -> Error.Name.OlmKeysNotSentError
            MXCryptoError.ErrorType.OLM                   -> Error.Name.OlmUnspecifiedError
            MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX -> Error.Name.OlmIndexError
            else                                          -> Error.Name.UnknownError
        }
    } else {
        Error.Name.UnknownError
    }
    // check if it's an expected UTD!
    val localAge = this.eventLocalAgeAtDecryptionFailure
    val isHistorical = localAge != null && localAge < 0
    if (isHistorical && !this.ownIdentityTrustedAtTimeOfDecryptionFailure) {
        return Error.Name.HistoricalMessage
    }

    return name
}
