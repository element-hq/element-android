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

package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.model.create.CreateRoomResponse
import im.vector.matrix.android.internal.network.NetworkConstants
import im.vector.matrix.android.internal.session.room.invite.InviteBody
import im.vector.matrix.android.internal.session.room.members.RoomMembersResponse
import im.vector.matrix.android.internal.session.room.send.SendResponse
import im.vector.matrix.android.internal.session.room.timeline.EventContextResponse
import im.vector.matrix.android.internal.session.room.timeline.PaginationResponse
import retrofit2.Call
import retrofit2.http.*

internal interface RoomAPI {

    /**
     * Create a room.
     * Ref: https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-createroom
     *
     * @param param the creation room parameter
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "createRoom")
    fun createRoom(@Body param: CreateRoomParams): Call<CreateRoomResponse>

    /**
     * Get a list of messages starting from a reference.
     *
     * @param roomId the room id
     * @param from   the token identifying where to start. Required.
     * @param dir    The direction to return messages from. Required.
     * @param limit  the maximum number of messages to retrieve. Optional.
     * @param filter A JSON RoomEventFilter to filter returned events with. Optional.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/messages")
    fun getRoomMessagesFrom(@Path("roomId") roomId: String,
                            @Query("from") from: String,
                            @Query("dir") dir: String,
                            @Query("limit") limit: Int,
                            @Query("filter") filter: String?
    ): Call<PaginationResponse>


    /**
     * Get all members of a room
     *
     * @param roomId        the room id where to get the members
     * @param syncToken     the sync token (optional)
     * @param membership    to include only one type of membership (optional)
     * @param notMembership to exclude one type of membership (optional)
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/members")
    fun getMembers(@Path("roomId") roomId: String,
                   @Query("at") syncToken: String?,
                   @Query("membership") membership: String?,
                   @Query("not_membership") notMembership: String?
    ): Call<RoomMembersResponse>


    /**
     * Send an event to a room.
     *
     * @param txId      the transaction Id
     * @param roomId    the room id
     * @param eventType the event type
     * @param content   the event content
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/send/{eventType}/{txId}")
    fun send(@Path("txId") txId: String,
             @Path("roomId") roomId: String,
             @Path("eventType") eventType: String,
             @Body content: Content?
    ): Call<SendResponse>

    /**
     * Get the context surrounding an event.
     *
     * @param roomId  the room id
     * @param eventId the event Id
     * @param limit   the maximum number of messages to retrieve
     * @param filter  A JSON RoomEventFilter to filter returned events with. Optional.
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/context/{eventId}")
    fun getContextOfEvent(@Path("roomId") roomId: String,
                          @Path("eventId") eventId: String,
                          @Query("limit") limit: Int,
                          @Query("filter") filter: String? = null): Call<EventContextResponse>

    /**
     * Retrieve an event from its room id / events id
     *
     * @param roomId  the room id
     * @param eventId the event Id
     */
    @GET(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/event/{eventId}")
    fun getEvent(@Path("roomId") roomId: String, @Path("eventId") eventId: String): Call<Event>

    /**
     * Send read markers.
     *
     * @param roomId  the room id
     * @param markers the read markers
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/read_markers")
    fun sendReadMarker(@Path("roomId") roomId: String, @Body markers: Map<String, String>): Call<Unit>

    /**
     * Invite a user to the given room.
     * Ref: https://matrix.org/docs/spec/client_server/r0.4.0.html#post-matrix-client-r0-rooms-roomid-invite
     *
     * @param roomId the room id
     * @param body   a object that just contains a user id
     */
    @POST(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/invite")
    fun invite(@Path("roomId") roomId: String, @Body body: InviteBody): Call<Unit>

    /**
     * Send a generic state events
     *
     * @param roomId         the room id.
     * @param stateEventType the state event type
     * @param params         the request parameters
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/state/{state_event_type}")
    fun sendStateEvent(@Path("roomId") roomId: String,
                       @Path("state_event_type") stateEventType: String,
                       @Body params: Map<String, String>): Call<Unit>

    /**
     * Send a generic state events
     *
     * @param roomId         the room id.
     * @param stateEventType the state event type
     * @param stateKey       the state keys
     * @param params         the request parameters
     */
    @PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/state/{state_event_type}/{state_key}")
    fun sendStateEvent(@Path("roomId") roomId: String,
                       @Path("state_event_type") stateEventType: String,
                       @Path("state_key") stateKey: String,
                       @Body params: Map<String, String>): Call<Unit>
}