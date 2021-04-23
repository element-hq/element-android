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

package org.matrix.android.sdk.internal.session.group

import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.group.model.GroupRooms
import org.matrix.android.sdk.internal.session.group.model.GroupSummaryResponse
import org.matrix.android.sdk.internal.session.group.model.GroupUsers
import retrofit2.http.GET
import retrofit2.http.Path

internal interface GroupAPI {

    /**
     * Request a group summary
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/summary")
    suspend fun getSummary(@Path("groupId") groupId: String): GroupSummaryResponse

    /**
     * Request the rooms list.
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/rooms")
    suspend fun getRooms(@Path("groupId") groupId: String): GroupRooms

    /**
     * Request the users list.
     *
     * @param groupId the group id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/users")
    suspend fun getUsers(@Path("groupId") groupId: String): GroupUsers
}
