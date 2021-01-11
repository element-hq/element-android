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

package org.matrix.android.sdk.internal.session.thirdparty

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsParams
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsResponse
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.network.NetworkConstants
import org.matrix.android.sdk.internal.session.room.alias.GetAliasesResponse
import org.matrix.android.sdk.internal.session.room.create.CreateRoomBody
import org.matrix.android.sdk.internal.session.room.create.CreateRoomResponse
import org.matrix.android.sdk.internal.session.room.create.JoinRoomResponse
import org.matrix.android.sdk.internal.session.room.membership.RoomMembersResponse
import org.matrix.android.sdk.internal.session.room.membership.admin.UserIdAndReason
import org.matrix.android.sdk.internal.session.room.membership.joining.InviteBody
import org.matrix.android.sdk.internal.session.room.membership.threepid.ThreePidInviteBody
import org.matrix.android.sdk.internal.session.room.relation.RelationsResponse
import org.matrix.android.sdk.internal.session.room.reporting.ReportContentBody
import org.matrix.android.sdk.internal.session.room.send.SendResponse
import org.matrix.android.sdk.internal.session.room.tags.TagBody
import org.matrix.android.sdk.internal.session.room.timeline.EventContextResponse
import org.matrix.android.sdk.internal.session.room.timeline.PaginationResponse
import org.matrix.android.sdk.internal.session.room.typing.TypingBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ThirdPartyAPI {

    /**
     * Get the third party server protocols.
     *
     * Ref: https://matrix.org/docs/spec/client_server/r0.4.0.html#get-matrix-client-r0-thirdparty-protocols
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/protocols")
    fun thirdPartyProtocols(): Call<Map<String, ThirdPartyProtocol>>

}
