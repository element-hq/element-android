/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.failure

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.internal.di.MoshiProvider
import java.io.IOException
import javax.net.ssl.HttpsURLConnection

fun Throwable.is401() =
        this is Failure.ServerError
                && httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED /* 401 */
                && error.code == MatrixError.M_UNAUTHORIZED

fun Throwable.isTokenError() =
        this is Failure.ServerError
                && (error.code == MatrixError.M_UNKNOWN_TOKEN || error.code == MatrixError.M_MISSING_TOKEN)

fun Throwable.shouldBeRetried(): Boolean {
    return this is Failure.NetworkConnection
            || this is IOException
            || (this is Failure.ServerError && error.code == MatrixError.M_LIMIT_EXCEEDED)
}

fun Throwable.isInvalidPassword(): Boolean {
    return this is Failure.ServerError
            && error.code == MatrixError.M_FORBIDDEN
            && error.message == "Invalid password"
}

/**
 * Try to convert to a RegistrationFlowResponse. Return null in the cases it's not possible
 */
fun Throwable.toRegistrationFlowResponse(): RegistrationFlowResponse? {
    return if (this is Failure.OtherServerError && this.httpCode == 401) {
        tryOrNull {
            MoshiProvider.providesMoshi()
                    .adapter(RegistrationFlowResponse::class.java)
                    .fromJson(this.errorBody)
        }
    } else {
        null
    }
}
