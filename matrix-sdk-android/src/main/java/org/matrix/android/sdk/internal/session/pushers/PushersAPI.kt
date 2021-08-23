/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.internal.network.NetworkConstants
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface PushersAPI {

    /**
     * Get the pushers for this user.
     *
     * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushers
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushers")
    suspend fun getPushers(): GetPushersResponse

    /**
     * This endpoint allows the creation, modification and deletion of pushers for this user ID.
     * The behaviour of this endpoint varies depending on the values in the JSON body.
     *
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-pushers-set
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushers/set")
    suspend fun setPusher(@Body jsonPusher: JsonPusher)
}
