/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.failure

import javax.net.ssl.HttpsURLConnection

fun Throwable.is401() =
        this is Failure.ServerError
                && httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED /* 401 */
                && error.code == MatrixError.M_UNAUTHORIZED

fun Throwable.isTokenError() =
        this is Failure.ServerError
                && (error.code == MatrixError.M_UNKNOWN_TOKEN || error.code == MatrixError.M_MISSING_TOKEN)
