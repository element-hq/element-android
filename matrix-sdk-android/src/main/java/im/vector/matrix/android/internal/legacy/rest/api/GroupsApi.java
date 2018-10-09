/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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
package im.vector.matrix.android.internal.legacy.rest.api;

import im.vector.matrix.android.internal.legacy.rest.model.group.AcceptGroupInvitationParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.AddGroupParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.CreateGroupParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.CreateGroupResponse;
import im.vector.matrix.android.internal.legacy.rest.model.group.GetGroupsResponse;
import im.vector.matrix.android.internal.legacy.rest.model.group.GetPublicisedGroupsResponse;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupInviteUserParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupInviteUserResponse;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupKickUserParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupProfile;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupRooms;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupSummary;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupUsers;
import im.vector.matrix.android.internal.legacy.rest.model.group.LeaveGroupParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.UpdatePubliciseParams;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * The groups API.
 */
public interface GroupsApi {

    /**
     * Create a group
     *
     * @param params the group creation params
     */
    @POST("create_group")
    Call<CreateGroupResponse> createGroup(@Body CreateGroupParams params);

    /**
     * Invite an user to a group.
     *
     * @param groupId the group id
     * @param userId  the user id
     * @param params  the invitation parameters
     */
    @PUT("groups/{groupId}/admin/users/invite/{userId}")
    Call<GroupInviteUserResponse> inviteUser(@Path("groupId") String groupId, @Path("userId") String userId, @Body GroupInviteUserParams params);

    /**
     * Kick an user from a group.
     *
     * @param groupId the group id
     * @param userId  the user id
     * @param params  the kick parameters
     */
    @PUT("groups/{groupId}/users/remove/{userId}")
    Call<Void> kickUser(@Path("groupId") String groupId, @Path("userId") String userId, @Body GroupKickUserParams params);

    /**
     * Add a room in a group.
     *
     * @param groupId the group id
     * @param roomId  the room id
     * @param params  the kick parameters
     */
    @PUT("groups/{groupId}/admin/rooms/{roomId}")
    Call<Void> addRoom(@Path("groupId") String groupId, @Path("roomId") String roomId, @Body AddGroupParams params);

    /**
     * Remove a room from a group.
     *
     * @param groupId the group id
     * @param roomId  the room id
     */
    @DELETE("groups/{groupId}/admin/rooms/{roomId}")
    Call<Void> removeRoom(@Path("groupId") String groupId, @Path("roomId") String roomId);

    /**
     * Update the group profile.
     *
     * @param groupId the group id
     * @param profile the group profile
     */
    @POST("groups/{groupId}/profile")
    Call<Void> updateProfile(@Path("groupId") String groupId, @Body GroupProfile profile);

    /**
     * Get the group profile.
     *
     * @param groupId the group id
     */
    @GET("groups/{groupId}/profile")
    Call<GroupProfile> getProfile(@Path("groupId") String groupId);

    /**
     * Request the invited users list.
     *
     * @param groupId the group id
     */
    @GET("groups/{groupId}/invited_users")
    Call<GroupUsers> getInvitedUsers(@Path("groupId") String groupId);

    /**
     * Request the users list.
     *
     * @param groupId the group id
     */
    @GET("groups/{groupId}/users")
    Call<GroupUsers> getUsers(@Path("groupId") String groupId);

    /**
     * Request the rooms list.
     *
     * @param groupId the group id
     */
    @GET("groups/{groupId}/rooms")
    Call<GroupRooms> getRooms(@Path("groupId") String groupId);

    /**
     * Request a group summary
     *
     * @param groupId the group id
     */
    @GET("groups/{groupId}/summary")
    Call<GroupSummary> getSummary(@Path("groupId") String groupId);

    /**
     * Accept an invitation in a group.
     *
     * @param groupId the group id
     * @param params  the parameters
     */
    @PUT("groups/{groupId}/self/accept_invite")
    Call<Void> acceptInvitation(@Path("groupId") String groupId, @Body AcceptGroupInvitationParams params);

    /**
     * Leave a group
     *
     * @param groupId the group id
     * @param params  the parameters
     */
    @PUT("groups/{groupId}/self/leave")
    Call<Void> leave(@Path("groupId") String groupId, @Body LeaveGroupParams params);

    /**
     * Update the publicity status.
     *
     * @param groupId the group id
     * @param params  the parameters
     */
    @PUT("groups/{groupId}/self/update_publicity")
    Call<Void> updatePublicity(@Path("groupId") String groupId, @Body UpdatePubliciseParams params);

    /**
     * Request the joined group list.
     */
    @GET("joined_groups")
    Call<GetGroupsResponse> getJoinedGroupIds();

    // NOT FEDERATED
    /**
     * Request the publicised groups for an user id.
     *
     * @param userId   the user id
     */
    //@GET("publicised_groups/{userId}")
    //Call<GetUserPublicisedGroupsResponse> getUserPublicisedGroups(@Path("userId") String userId);

    /**
     * Request the publicised groups for user ids.
     *
     * @param params the request params
     */
    @POST("publicised_groups")
    Call<GetPublicisedGroupsResponse> getPublicisedGroups(@Body Map<String, List<String>> params);
}
