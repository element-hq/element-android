/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.groups;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.MXPatterns;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.client.GroupsRestClient;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.group.CreateGroupParams;
import im.vector.matrix.android.internal.legacy.rest.model.group.Group;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupProfile;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupRooms;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupSummary;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupSyncProfile;
import im.vector.matrix.android.internal.legacy.rest.model.group.GroupUsers;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class manages the groups
 */
public class GroupsManager {
    private static final String LOG_TAG = GroupsManager.class.getSimpleName();

    private MXDataHandler mDataHandler;
    private GroupsRestClient mGroupsRestClient;
    private IMXStore mStore;

    // callbacks
    private Set<ApiCallback<Void>> mRefreshProfilesCallback = new HashSet<>();

    //
    private final Map<String, ApiCallback<Void>> mPendingJoinGroups = new HashMap<>();
    private final Map<String, ApiCallback<Void>> mPendingLeaveGroups = new HashMap<>();

    // publicise management
    private Map<String, Set<ApiCallback<Set<String>>>> mPendingPubliciseRequests = new HashMap<>();
    private Map<String, Set<String>> mPubliciseByUserId = new HashMap<>();

    private Handler mUIHandler;

    /**
     * Constructor
     *
     * @param dataHandler the data handler
     * @param restClient  the group rest client
     */
    public GroupsManager(MXDataHandler dataHandler, GroupsRestClient restClient) {
        mDataHandler = dataHandler;
        mStore = mDataHandler.getStore();
        mGroupsRestClient = restClient;

        mUIHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * @return the groups rest client
     */
    public GroupsRestClient getGroupsRestClient() {
        return mGroupsRestClient;
    }


    /**
     * Call when the session is paused
     */
    public void onSessionPaused() {
        mPubliciseByUserId.clear();
    }

    /**
     * Call when the session is resumed
     */
    public void onSessionResumed() {
        refreshGroupProfiles((ApiCallback<Void>) null);
        getUserPublicisedGroups(mDataHandler.getUserId(), true, new SimpleApiCallback<Set<String>>() {
            @Override
            public void onSuccess(Set<String> info) {
                // Ignore
            }
        });

        mGroupProfileByGroupId.clear();
        mGroupProfileCallback.clear();
    }

    /**
     * Retrieve the group from a group id
     *
     * @param groupId the group id
     * @return the group if it exists
     */
    public Group getGroup(String groupId) {
        return mStore.getGroup(groupId);
    }

    /**
     * @return the existing groups
     */
    public Collection<Group> getGroups() {
        return mStore.getGroups();
    }

    /**
     * @return the groups list in which the user is invited
     */
    public Collection<Group> getInvitedGroups() {
        List<Group> invitedGroups = new ArrayList<>();
        Collection<Group> groups = getGroups();

        for (Group group : groups) {
            if (group.isInvited()) {
                invitedGroups.add(group);
            }
        }

        return invitedGroups;
    }

    /**
     * @return the joined groups
     */
    public Collection<Group> getJoinedGroups() {
        List<Group> joinedGroups = new ArrayList<>(getGroups());
        joinedGroups.removeAll(getInvitedGroups());

        return joinedGroups;
    }

    /**
     * Manage the group joining.
     *
     * @param groupId the group id
     * @param notify  true to notify
     */
    public void onJoinGroup(final String groupId, final boolean notify) {
        Group group = getGroup(groupId);

        if (null == group) {
            group = new Group(groupId);
        }

        if (TextUtils.equals(RoomMember.MEMBERSHIP_JOIN, group.getMembership())) {
            Log.d(LOG_TAG, "## onJoinGroup() : the group " + groupId + " was already joined");
            return;
        }

        group.setMembership(RoomMember.MEMBERSHIP_JOIN);
        mStore.storeGroup(group);

        // try retrieve  the summary
        mGroupsRestClient.getGroupSummary(groupId, new ApiCallback<GroupSummary>() {
            /**
             * Common method
             */
            private void onDone() {
                if (notify) {
                    mDataHandler.onJoinGroup(groupId);
                }
            }

            @Override
            public void onSuccess(GroupSummary groupSummary) {
                Group group = getGroup(groupId);

                if (null != group) {
                    group.setGroupSummary(groupSummary);
                    mStore.flushGroup(group);
                    onDone();

                    if (null != mPendingJoinGroups.get(groupId)) {
                        mPendingJoinGroups.get(groupId).onSuccess(null);
                        mPendingJoinGroups.remove(groupId);
                    }
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## onJoinGroup() : failed " + e.getMessage(), e);
                onDone();

                if (null != mPendingJoinGroups.get(groupId)) {
                    mPendingJoinGroups.get(groupId).onNetworkError(e);
                    mPendingJoinGroups.remove(groupId);
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## onMatrixError() : failed " + e.getMessage());
                onDone();

                if (null != mPendingJoinGroups.get(groupId)) {
                    mPendingJoinGroups.get(groupId).onMatrixError(e);
                    mPendingJoinGroups.remove(groupId);
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## onUnexpectedError() : failed " + e.getMessage(), e);
                onDone();

                if (null != mPendingJoinGroups.get(groupId)) {
                    mPendingJoinGroups.get(groupId).onUnexpectedError(e);
                    mPendingJoinGroups.remove(groupId);
                }
            }
        });
    }

    /**
     * Create a group from an invitation.
     *
     * @param groupId the group id
     * @param profile the profile
     * @param inviter the inviter
     * @param notify  true to notify
     */
    public void onNewGroupInvitation(final String groupId, final GroupSyncProfile profile, final String inviter, final boolean notify) {
        Group group = getGroup(groupId);

        // it should always be null
        if (null == group) {
            group = new Group(groupId);
        }

        GroupSummary summary = new GroupSummary();
        summary.profile = new GroupProfile();
        if (null != profile) {
            summary.profile.name = profile.name;
            summary.profile.avatarUrl = profile.avatarUrl;
        }

        group.setGroupSummary(summary);
        group.setInviter(inviter);
        group.setMembership(RoomMember.MEMBERSHIP_INVITE);

        mStore.storeGroup(group);

        if (notify) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDataHandler.onNewGroupInvitation(groupId);
                }
            });
        }
    }

    /**
     * Remove a group.
     *
     * @param groupId the group id.
     * @param notify  true to notify
     */
    public void onLeaveGroup(final String groupId, final boolean notify) {
        if (null != mStore.getGroup(groupId)) {
            mStore.deleteGroup(groupId);

            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (notify) {
                        mDataHandler.onLeaveGroup(groupId);
                    }

                    if (mPendingLeaveGroups.containsKey(groupId)) {
                        mPendingLeaveGroups.get(groupId).onSuccess(null);
                        mPendingLeaveGroups.remove(groupId);
                    }
                }
            });
        }
    }

    /**
     * Refresh the group profiles
     *
     * @param callback the asynchronous callback
     */
    public void refreshGroupProfiles(ApiCallback<Void> callback) {
        if (!mRefreshProfilesCallback.isEmpty()) {
            Log.d(LOG_TAG, "## refreshGroupProfiles() : there already is a pending request");
            mRefreshProfilesCallback.add(callback);
            return;
        }

        mRefreshProfilesCallback.add(callback);
        refreshGroupProfiles(getGroups().iterator());
    }

    /**
     * Internal method to refresh the group profiles.
     *
     * @param iterator the iterator.
     */
    private void refreshGroupProfiles(final Iterator<Group> iterator) {
        if (!iterator.hasNext()) {
            for (ApiCallback<Void> callback : mRefreshProfilesCallback) {
                try {
                    if (null != callback) {
                        callback.onSuccess(null);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## refreshGroupProfiles() failed " + e.getMessage(), e);
                }
            }
            mRefreshProfilesCallback.clear();
            return;
        }

        final String groupId = iterator.next().getGroupId();

        mGroupsRestClient.getGroupProfile(groupId, new ApiCallback<GroupProfile>() {
            private void onDone() {
                refreshGroupProfiles(iterator);
            }

            @Override
            public void onSuccess(GroupProfile profile) {
                Group group = getGroup(groupId);

                if (null != group) {
                    group.setGroupProfile(profile);
                    mStore.flushGroup(group);
                }

                mDataHandler.onGroupProfileUpdate(groupId);
                mGroupProfileByGroupId.put(groupId, profile);
                onDone();
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## refreshGroupProfiles() : failed " + e.getMessage(), e);
                onDone();
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## refreshGroupProfiles() : failed " + e.getMessage());
                onDone();
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## refreshGroupProfiles() : failed " + e.getMessage(), e);
                onDone();
            }
        });
    }

    /**
     * Join a group.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback
     */
    public void joinGroup(final String groupId, final ApiCallback<Void> callback) {
        getGroupsRestClient().joinGroup(groupId, new SimpleApiCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                Group group = getGroup(groupId);
                // not yet synced -> wait it is synced
                if ((null == group) || TextUtils.equals(group.getMembership(), RoomMember.MEMBERSHIP_INVITE)) {
                    mPendingJoinGroups.put(groupId, callback);
                    onJoinGroup(groupId, true);
                } else {
                    callback.onSuccess(null);
                }
            }
        });
    }

    /**
     * Leave a group.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback
     */
    public void leaveGroup(final String groupId, final ApiCallback<Void> callback) {
        getGroupsRestClient().leaveGroup(groupId, new SimpleApiCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                Group group = getGroup(groupId);
                // not yet synced -> wait it is synced
                if (null != group) {
                    mPendingLeaveGroups.put(groupId, callback);
                    onLeaveGroup(groupId, true);
                } else {
                    callback.onSuccess(null);
                }
            }
        });
    }

    /**
     * Create a group.
     *
     * @param localPart the local part
     * @param groupName the group human name
     * @param callback  the asynchronous callback
     */
    public void createGroup(String localPart, String groupName, final ApiCallback<String> callback) {
        final CreateGroupParams params = new CreateGroupParams();
        params.localpart = localPart;
        params.profile = new GroupProfile();
        params.profile.name = groupName;

        getGroupsRestClient().createGroup(params, new SimpleApiCallback<String>(callback) {
            @Override
            public void onSuccess(String groupId) {
                Group group = getGroup(groupId);

                // if the group does not exist, create it
                if (null == group) {
                    group = new Group(groupId);
                    group.setGroupProfile(params.profile);
                    group.setMembership(RoomMember.MEMBERSHIP_JOIN);
                    mStore.storeGroup(group);
                }

                callback.onSuccess(groupId);
            }
        });
    }

    /**
     * Refresh the group data i.e the invited users list, the users list and the rooms list.
     *
     * @param group    the group
     * @param callback the asynchronous callback
     */
    public void refreshGroupData(Group group, ApiCallback<Void> callback) {
        refreshGroupData(group, GROUP_REFRESH_STEP_PROFILE, callback);
    }

    private static final int GROUP_REFRESH_STEP_PROFILE = 0;
    private static final int GROUP_REFRESH_STEP_ROOMS_LIST = 1;
    private static final int GROUP_REFRESH_STEP_USERS_LIST = 2;
    private static final int GROUP_REFRESH_STEP_INVITED_USERS_LIST = 3;

    /**
     * Internal method to refresh the group informations.
     *
     * @param group    the group
     * @param step     the current step
     * @param callback the asynchronous callback
     */
    private void refreshGroupData(final Group group, final int step, final ApiCallback<Void> callback) {
        if (step == GROUP_REFRESH_STEP_PROFILE) {
            getGroupsRestClient().getGroupProfile(group.getGroupId(), new SimpleApiCallback<GroupProfile>(callback) {
                @Override
                public void onSuccess(GroupProfile groupProfile) {
                    group.setGroupProfile(groupProfile);
                    mStore.flushGroup(group);
                    mDataHandler.onGroupProfileUpdate(group.getGroupId());
                    refreshGroupData(group, GROUP_REFRESH_STEP_ROOMS_LIST, callback);
                }
            });

            return;
        }

        if (step == GROUP_REFRESH_STEP_ROOMS_LIST) {
            getGroupsRestClient().getGroupRooms(group.getGroupId(), new SimpleApiCallback<GroupRooms>(callback) {
                @Override
                public void onSuccess(GroupRooms groupRooms) {
                    group.setGroupRooms(groupRooms);
                    mStore.flushGroup(group);
                    mDataHandler.onGroupRoomsListUpdate(group.getGroupId());
                    refreshGroupData(group, GROUP_REFRESH_STEP_USERS_LIST, callback);
                }
            });
            return;
        }

        if (step == GROUP_REFRESH_STEP_USERS_LIST) {
            getGroupsRestClient().getGroupUsers(group.getGroupId(), new SimpleApiCallback<GroupUsers>(callback) {
                @Override
                public void onSuccess(GroupUsers groupUsers) {
                    group.setGroupUsers(groupUsers);
                    mStore.flushGroup(group);
                    mDataHandler.onGroupUsersListUpdate(group.getGroupId());
                    refreshGroupData(group, GROUP_REFRESH_STEP_INVITED_USERS_LIST, callback);
                }
            });
            return;
        }


        //if (step == GROUP_REFRESH_STEP_INVITED_USERS_LIST)

        getGroupsRestClient().getGroupInvitedUsers(group.getGroupId(), new SimpleApiCallback<GroupUsers>(callback) {
            @Override
            public void onSuccess(GroupUsers groupUsers) {
                group.setInvitedGroupUsers(groupUsers);

                if (null != mStore.getGroup(group.getGroupId())) {
                    mStore.flushGroup(group);
                }
                mDataHandler.onGroupInvitedUsersListUpdate(group.getGroupId());
                callback.onSuccess(null);
            }
        });
    }

    /**
     * Retrieves the cached publicisedGroups for an userId.
     *
     * @param userId the user id
     * @return a set if there is a cached one, else null
     */
    public Set<String> getUserPublicisedGroups(final String userId) {
        if (mPubliciseByUserId.containsKey(userId)) {
            return new HashSet<>(mPubliciseByUserId.get(userId));
        }

        return null;
    }

    /**
     * Request the publicised groups for an user.
     *
     * @param userId       the user id
     * @param forceRefresh true to do not use the cached data
     * @param callback     the asynchronous callback.
     */
    public void getUserPublicisedGroups(final String userId,
                                        final boolean forceRefresh,
                                        @NonNull final ApiCallback<Set<String>> callback) {
        Log.d(LOG_TAG, "## getUserPublicisedGroups() : " + userId);

        // sanity check
        if (!MXPatterns.isUserId(userId)) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(new HashSet<String>());
                }
            });

            return;
        }

        // already cached
        if (forceRefresh) {
            mPubliciseByUserId.remove(userId);
        } else {
            if (mPubliciseByUserId.containsKey(userId)) {
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "## getUserPublicisedGroups() : " + userId + " --> cached data " + mPubliciseByUserId.get(userId));
                        // reported by a rage shake
                        if (mPubliciseByUserId.containsKey(userId)) {
                            callback.onSuccess(new HashSet<>(mPubliciseByUserId.get(userId)));
                        } else {
                            callback.onSuccess(new HashSet<String>());
                        }
                    }
                });

                return;
            }
        }

        // request in progress
        if (mPendingPubliciseRequests.containsKey(userId)) {
            Log.d(LOG_TAG, "## getUserPublicisedGroups() : " + userId + " request in progress");
            mPendingPubliciseRequests.get(userId).add(callback);
            return;
        }

        mPendingPubliciseRequests.put(userId, new HashSet<ApiCallback<Set<String>>>());
        mPendingPubliciseRequests.get(userId).add(callback);

        mGroupsRestClient.getUserPublicisedGroups(userId, new ApiCallback<List<String>>() {
            private void onDone(Set<String> groupIdsSet) {

                // cache only if the request succeeds
                // else it will be tried later
                if (null != groupIdsSet) {
                    mPubliciseByUserId.put(userId, new HashSet<>(groupIdsSet));
                } else {
                    groupIdsSet = new HashSet<>();
                }

                Log.d(LOG_TAG, "## getUserPublicisedGroups() : " + userId + " -- " + groupIdsSet);

                Set<ApiCallback<Set<String>>> callbacks = mPendingPubliciseRequests.get(userId);
                mPendingPubliciseRequests.remove(userId);

                if (null != callbacks) {
                    for (ApiCallback<Set<String>> callback : callbacks) {
                        if (null != callback) {
                            try {
                                callback.onSuccess(new HashSet<>(groupIdsSet));
                            } catch (Throwable t) {
                                Log.d(LOG_TAG, "## getUserPublicisedGroups() : callback failed " + t.getMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void onSuccess(List<String> groupIdsList) {
                onDone((null == groupIdsList) ? new HashSet<String>() : new HashSet<>(groupIdsList));
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(LOG_TAG, "## getUserPublicisedGroups() : request failed " + e.getMessage(), e);
                onDone(null);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(LOG_TAG, "## getUserPublicisedGroups() : request failed " + e.getMessage());
                onDone(null);
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(LOG_TAG, "## getUserPublicisedGroups() : request failed " + e.getMessage(), e);
                onDone(null);
            }
        });
    }

    /**
     * Update a group publicity status.
     *
     * @param groupId   the group id
     * @param publicity the new publicity status
     * @param callback  the asynchronous callback.
     */
    public void updateGroupPublicity(final String groupId, final boolean publicity, final ApiCallback<Void> callback) {
        getGroupsRestClient().updateGroupPublicity(groupId, publicity, new SimpleApiCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                if (mPubliciseByUserId.containsKey(groupId)) {
                    if (publicity) {
                        mPubliciseByUserId.get(groupId).add(groupId);
                    } else {
                        mPubliciseByUserId.get(groupId).remove(groupId);
                    }
                }

                if (null != callback) {
                    callback.onSuccess(null);
                }
            }
        });
    }

    Map<String, GroupProfile> mGroupProfileByGroupId = new HashMap<>();
    Map<String, List<ApiCallback<GroupProfile>>> mGroupProfileCallback = new HashMap<>();


    /**
     * Retrieve the cached group profile
     *
     * @param groupId the group id
     * @return the cached GroupProfile if it exits, else null
     */
    public GroupProfile getGroupProfile(final String groupId) {
        return mGroupProfileByGroupId.get(groupId);
    }

    /**
     * Request the profile of a group.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback
     */
    public void getGroupProfile(final String groupId, final ApiCallback<GroupProfile> callback) {
        // sanity check
        if (null == callback) {
            return;
        }

        // valid group id
        if (TextUtils.isEmpty(groupId) || !MXPatterns.isGroupId(groupId)) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(new GroupProfile());
                }
            });

            return;
        }

        // already downloaded
        if (mGroupProfileByGroupId.containsKey(groupId)) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(mGroupProfileByGroupId.get(groupId));
                }
            });

            return;
        }

        // in progress
        if (mGroupProfileCallback.containsKey(groupId)) {
            mGroupProfileCallback.get(groupId).add(callback);
            return;
        }

        mGroupProfileCallback.put(groupId, new ArrayList<>(Arrays.asList(callback)));

        mGroupsRestClient.getGroupProfile(groupId, new ApiCallback<GroupProfile>() {
            @Override
            public void onSuccess(GroupProfile groupProfile) {
                mGroupProfileByGroupId.put(groupId, groupProfile);
                List<ApiCallback<GroupProfile>> callbacks = mGroupProfileCallback.get(groupId);
                mGroupProfileCallback.remove(groupId);

                if (null != callbacks) {
                    for (ApiCallback<GroupProfile> c : callbacks) {
                        c.onSuccess(groupProfile);
                    }
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                List<ApiCallback<GroupProfile>> callbacks = mGroupProfileCallback.get(groupId);
                mGroupProfileCallback.remove(groupId);

                if (null != callbacks) {
                    for (ApiCallback<GroupProfile> c : callbacks) {
                        c.onNetworkError(e);
                    }
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                List<ApiCallback<GroupProfile>> callbacks = mGroupProfileCallback.get(groupId);
                mGroupProfileCallback.remove(groupId);

                if (null != callbacks) {
                    for (ApiCallback<GroupProfile> c : callbacks) {
                        c.onMatrixError(e);
                    }
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                List<ApiCallback<GroupProfile>> callbacks = mGroupProfileCallback.get(groupId);
                mGroupProfileCallback.remove(groupId);

                if (null != callbacks) {
                    for (ApiCallback<GroupProfile> c : callbacks) {
                        c.onUnexpectedError(e);
                    }
                }
            }
        });

    }
}
