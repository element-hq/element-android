/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.legacy.rest.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.vector.matrix.android.api.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.rest.api.GroupsApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.RestAdapterCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
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
import retrofit2.Response;

/**
 * Class used to make requests to the groups API.
 */
public class GroupsRestClient extends RestClient<GroupsApi> {

    /**
     * {@inheritDoc}
     */
    public GroupsRestClient(SessionParams sessionParams) {
        super(sessionParams, GroupsApi.class, RestClient.URI_API_PREFIX_PATH_R0, false);
    }

    protected GroupsRestClient(GroupsApi api) {
        mApi = api;
    }

    /**
     * Create a group.
     *
     * @param params   the room creation parameters
     * @param callback the asynchronous callback.
     */
    public void createGroup(final CreateGroupParams params, final ApiCallback<String> callback) {
        final String description = "createGroup " + params.localpart;

        mApi.createGroup(params)
                .enqueue(new RestAdapterCallback<CreateGroupResponse>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                createGroup(params, callback);
            }
        }) {
            @Override
            public void success(CreateGroupResponse createGroupResponse, Response<CreateGroupResponse> response) {
                onEventSent();
                callback.onSuccess(createGroupResponse.group_id);
            }
        });
    }

    /**
     * Invite an user in a group.
     *
     * @param groupId  the group id
     * @param userId   the user id
     * @param callback the asynchronous callback.
     */
    public void inviteUserInGroup(final String groupId, final String userId, final ApiCallback<String> callback) {
        final String description = "inviteUserInGroup " + groupId + " - " + userId;

        GroupInviteUserParams params = new GroupInviteUserParams();

        mApi.inviteUser(groupId, userId, params)
                .enqueue(new RestAdapterCallback<GroupInviteUserResponse>(description, mUnsentEventsManager, callback,
                new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                inviteUserInGroup(groupId, userId, callback);
            }
        }) {
            @Override
            public void success(GroupInviteUserResponse groupInviteUserResponse, Response<GroupInviteUserResponse> response) {
                onEventSent();
                callback.onSuccess(groupInviteUserResponse.state);
            }
        });
    }

    /**
     * Kick an user from a group.
     *
     * @param groupId  the group id
     * @param userId   the user id
     * @param callback the asynchronous callback.
     */
    public void KickUserFromGroup(final String groupId, final String userId, final ApiCallback<Void> callback) {
        final String description = "KickFromGroup " + groupId + " " + userId;

        GroupKickUserParams params = new GroupKickUserParams();

        mApi.kickUser(groupId, userId, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                KickUserFromGroup(groupId, userId, callback);
            }
        }));
    }

    /**
     * Add a room in a group.
     *
     * @param groupId  the group id
     * @param roomId   the room id
     * @param callback the asynchronous callback.
     */
    public void addRoomInGroup(final String groupId, final String roomId, final ApiCallback<Void> callback) {
        final String description = "addRoomInGroup " + groupId + " " + roomId;

        AddGroupParams params = new AddGroupParams();

        mApi.addRoom(groupId, roomId, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                addRoomInGroup(groupId, roomId, callback);
            }
        }));
    }

    /**
     * Remove a room from a group.
     *
     * @param groupId  the group id
     * @param roomId   the room id
     * @param callback the asynchronous callback.
     */
    public void removeRoomFromGroup(final String groupId, final String roomId, final ApiCallback<Void> callback) {
        final String description = "removeRoomFromGroup " + groupId + " " + roomId;

        mApi.removeRoom(groupId, roomId)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                removeRoomFromGroup(groupId, roomId, callback);
            }
        }));
    }

    /**
     * Update a group profile.
     *
     * @param groupId  the group id
     * @param profile  the profile
     * @param callback the asynchronous callback.
     */
    public void updateGroupProfile(final String groupId, final GroupProfile profile, final ApiCallback<Void> callback) {
        final String description = "updateGroupProfile " + groupId;

        mApi.updateProfile(groupId, profile)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                updateGroupProfile(groupId, profile, callback);
            }
        }));
    }

    /**
     * Update a group profile.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void getGroupProfile(final String groupId, final ApiCallback<GroupProfile> callback) {
        final String description = "getGroupProfile " + groupId;

        mApi.getProfile(groupId)
                .enqueue(new RestAdapterCallback<GroupProfile>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getGroupProfile(groupId, callback);
            }
        }));
    }

    /**
     * Request the group invited users.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void getGroupInvitedUsers(final String groupId, final ApiCallback<GroupUsers> callback) {
        final String description = "getGroupInvitedUsers " + groupId;

        mApi.getInvitedUsers(groupId)
                .enqueue(new RestAdapterCallback<GroupUsers>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getGroupInvitedUsers(groupId, callback);
            }
        }));
    }

    /**
     * Request the group rooms.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void getGroupRooms(final String groupId, final ApiCallback<GroupRooms> callback) {
        final String description = "getGroupRooms " + groupId;

        mApi.getRooms(groupId)
                .enqueue(new RestAdapterCallback<GroupRooms>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getGroupRooms(groupId, callback);
            }
        }));
    }

    /**
     * Request the group users.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void getGroupUsers(final String groupId, final ApiCallback<GroupUsers> callback) {
        final String description = "getGroupUsers " + groupId;

        mApi.getUsers(groupId)
                .enqueue(new RestAdapterCallback<GroupUsers>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getGroupUsers(groupId, callback);
            }
        }));
    }

    /**
     * Request a group summary
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void getGroupSummary(final String groupId, final ApiCallback<GroupSummary> callback) {
        final String description = "getGroupSummary " + groupId;

        mApi.getSummary(groupId)
                .enqueue(new RestAdapterCallback<GroupSummary>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getGroupSummary(groupId, callback);
            }
        }));
    }

    /**
     * Join a group.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void joinGroup(final String groupId, final ApiCallback<Void> callback) {
        final String description = "joinGroup " + groupId;

        AcceptGroupInvitationParams params = new AcceptGroupInvitationParams();

        mApi.acceptInvitation(groupId, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                joinGroup(groupId, callback);
            }
        }));
    }

    /**
     * Leave a group.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback.
     */
    public void leaveGroup(final String groupId, final ApiCallback<Void> callback) {
        final String description = "leaveGroup " + groupId;

        LeaveGroupParams params = new LeaveGroupParams();

        mApi.leave(groupId, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                leaveGroup(groupId, callback);
            }
        }));
    }

    /**
     * Update a group publicity status.
     *
     * @param groupId   the group id
     * @param publicity the new publicity status
     * @param callback  the asynchronous callback.
     */
    public void updateGroupPublicity(final String groupId, final boolean publicity, final ApiCallback<Void> callback) {
        final String description = "updateGroupPublicity " + groupId + " - " + publicity;

        UpdatePubliciseParams params = new UpdatePubliciseParams();
        params.publicise = publicity;

        mApi.updatePublicity(groupId, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                updateGroupPublicity(groupId, publicity, callback);
            }
        }));
    }

    /**
     * Request the joined groups.
     *
     * @param callback the asynchronous callback.
     */
    public void getJoinedGroups(final ApiCallback<List<String>> callback) {
        final String description = "getJoinedGroups";

        mApi.getJoinedGroupIds()
                .enqueue(new RestAdapterCallback<GetGroupsResponse>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getJoinedGroups(callback);
            }
        }) {
            @Override
            public void success(GetGroupsResponse getGroupsResponse, Response<GetGroupsResponse> response) {
                onEventSent();
                callback.onSuccess(getGroupsResponse.groupIds);
            }
        });
    }

    /**
     * Request the publicised groups for an user.
     *
     * @param userId   the user id
     * @param callback the asynchronous callback.
     */
    public void getUserPublicisedGroups(final String userId, final ApiCallback<List<String>> callback) {
        getPublicisedGroups(Arrays.asList(userId), new SimpleApiCallback<Map<String,List<String>>>(callback) {
            @Override
            public void onSuccess(Map<String, List<String>> map) {
                callback.onSuccess(map.get(userId));
            }
        });
    }

    /**
     * Request the publicised groups for an users list.
     *
     * @param userIds  the user ids list
     * @param callback the asynchronous callback
     */
    public void getPublicisedGroups(final List<String> userIds, final ApiCallback<Map<String, List<String>>> callback) {
        final String description = "getPublicisedGroups " + userIds;

        Map<String, List<String>> params = new HashMap<>();
        params.put("user_ids", userIds);

        mApi.getPublicisedGroups(params)
                .enqueue(new RestAdapterCallback<GetPublicisedGroupsResponse>(description, mUnsentEventsManager, callback,
                new RestAdapterCallback.RequestRetryCallBack() {
            @Override
            public void onRetry() {
                getPublicisedGroups(userIds, callback);
            }
        }
        ) {
            @Override
            public void success(GetPublicisedGroupsResponse getPublicisedGroupsResponse, Response<GetPublicisedGroupsResponse> response) {
                onEventSent();

                Map<String, List<String>> map = new HashMap<>();

                for (String userId : userIds) {
                    List<String> groupIds = null;

                    if ((null != getPublicisedGroupsResponse.users) && getPublicisedGroupsResponse.users.containsKey(userId)) {
                        groupIds = getPublicisedGroupsResponse.users.get(userId);
                    }

                    if (null == groupIds) {
                        groupIds = new ArrayList<>();
                    }

                    map.put(userId, groupIds);
                }

                callback.onSuccess(map);
            }
        });
    }
}
