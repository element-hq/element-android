/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.failure

import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import java.io.IOException

/**
 * This class allows to expose different kinds of error to be then handled by the application.
 * As it is a sealed class, you typically use it like that :
 * when(failure) {
 *   is NetworkConnection -> Unit
 *   is ServerError       -> Unit
 *   is Unknown           -> Unit
 * }
 */
sealed class Failure(cause: Throwable? = null) : Throwable(cause = cause) {
    data class Unknown(val throwable: Throwable? = null) : Failure(throwable)
    data class UnrecognizedCertificateFailure(val url: String, val fingerprint: Fingerprint) : Failure()
    data class NetworkConnection(val ioException: IOException? = null) : Failure(ioException)
    data class ServerError(val error: MatrixError, val httpCode: Int) : Failure(RuntimeException(error.toString()))
    object SuccessError : Failure(RuntimeException(RuntimeException("SuccessResult is false")))

    /**
     * When server send an error, but it cannot be interpreted as a MatrixError.
     */
    data class OtherServerError(val errorBody: String, val httpCode: Int) : Failure(RuntimeException("HTTP $httpCode: $errorBody"))

    data class RegistrationFlowError(val registrationFlowResponse: RegistrationFlowResponse) : Failure(RuntimeException(registrationFlowResponse.toString()))

    data class CryptoError(val error: MXCryptoError) : Failure(error)

    abstract class FeatureFailure : Failure()
}
