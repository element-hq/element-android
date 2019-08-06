/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.user

import im.vector.matrix.android.internal.network.NetworkConstants.URI_API_PREFIX_PATH_R0
import im.vector.matrix.android.internal.session.user.model.SearchUsersParams
import im.vector.matrix.android.internal.session.user.model.SearchUsersRequestResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

internal interface SearchUserAPI {

    /**
     * Perform a user search.
     *
     * @param searchUsersParams the search params.
     */
    @POST(URI_API_PREFIX_PATH_R0 + "user_directory/search")
    fun searchUsers(@Body searchUsersParams: SearchUsersParams): Call<SearchUsersRequestResponse>
}