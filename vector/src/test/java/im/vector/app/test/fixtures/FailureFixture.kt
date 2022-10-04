/*
 * Copyright (c) 2022 New Vector Ltd
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
