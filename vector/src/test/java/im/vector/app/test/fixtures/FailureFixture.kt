/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection

fun a401ServerError() = Failure.ServerError(
        MatrixError(MatrixError.M_UNAUTHORIZED, ""), HttpsURLConnection.HTTP_UNAUTHORIZED
)

fun anInvalidUserNameError() = Failure.ServerError(
        MatrixError(MatrixError.M_INVALID_USERNAME, ""), HttpsURLConnection.HTTP_BAD_REQUEST
)

fun anInvalidPasswordError() = Failure.ServerError(
        MatrixError(MatrixError.M_FORBIDDEN, "Invalid password"), HttpsURLConnection.HTTP_FORBIDDEN
)

fun aLoginEmailUnknownError() = Failure.ServerError(
        MatrixError(MatrixError.M_FORBIDDEN, ""), HttpsURLConnection.HTTP_FORBIDDEN
)

fun aHomeserverUnavailableError() = Failure.NetworkConnection(UnknownHostException())

fun anUnrecognisedCertificateError() = Failure.UnrecognizedCertificateFailure("a-url", Fingerprint(ByteArray(1), Fingerprint.HashType.SHA1))
