/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.group;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This class represents a community in Matrix.
 */
public class Group implements Serializable {
    /**
     * Sort by group id
     */
    public static final Comparator<Group> mGroupsComparator = new Comparator<Group>() {
        public int compare(Group group1, Group group2) {
            return group1.getGroupId().compareTo(group2.getGroupId());
        }
    };

    /**
     * The group id.
     */
    private String mGroupId;

    /**
     * The community summary.
     */
    private GroupSummary mSummary = new GroupSummary();

    /**
     * The rooms of the community.
     */
    private GroupRooms mRooms = new GroupRooms();

    /**
     * The community members.
     */
    private GroupUsers mUsers = new GroupUsers();

    /**
     * The community invited members.
     */
    private GroupUsers mInvitedUsers = new GroupUsers();

    /**
     * The user membership.
     */
    private String mMembership;

    /**
     * The identifier of the potential inviter (tells wether an invite is pending for this group).
     */
    private String mInviter;

    /**
     * Create an instance with a group id.
     *
     * @param groupId the identifier.
     * @return the MXGroup instance.
     */
    public Group(String groupId) {
        mGroupId = groupId;
    }

    /**
     * @return the group ID
     */
    public String getGroupId() {
        return mGroupId;
    }

    /**
     * Update the group profile.
     *
     * @param profile the group profile.
     */
    public void setGroupProfile(GroupProfile profile) {
        if (null == mSummary) {
            mSummary = new GroupSummary();
        }

        getGroupSummary().profile = profile;
    }

    /**
     * @return the group profile
     */
    public GroupProfile getGroupProfile() {
        if (null != getGroupSummary()) {
            return getGroupSummary().profile;
        }

        return null;
    }

    /**
     * @return the group name
     */
    public String getDisplayName() {
        String name = null;

        if (null != getGroupProfile()) {
            name = getGroupProfile().name;
        }

        if (TextUtils.isEmpty(name)) {
            name = getGroupId();
        }

        return name;
    }

    /**
     * @return the group long description
     */
    public String getLongDescription() {
        if (null != getGroupProfile()) {
            return getGroupProfile().longDescription;
        }

        return null;
    }

    /**
     * @return the avatar URL
     */
    public String getAvatarUrl() {
        if (null != getGroupProfile()) {
            return getGroupProfile().avatarUrl;
        }

        return null;
    }

    /**
     * @return the short description
     */
    public String getShortDescription() {
        if (null != getGroupProfile()) {
            return getGroupProfile().shortDescription;
        }

        return null;
    }

    /**
     * Tells if the group is public.
     *
     * @return true if the group is public.
     */
    public boolean isPublic() {
        return (null != getGroupProfile()) && (null != getGroupProfile().isPublic) && getGroupProfile().isPublic;
    }

    /**
     * Tells if the user is invited to this group.
     *
     * @return true if the user is invited
     */
    public boolean isInvited() {
        return TextUtils.equals(mMembership, RoomMember.MEMBERSHIP_INVITE);
    }

    /**
     * @return the group summary
     */
    public GroupSummary getGroupSummary() {
        return mSummary;
    }

    /**
     * Update the group summary
     *
     * @param aGroupSummary the new group summary
     */
    public void setGroupSummary(GroupSummary aGroupSummary) {
        mSummary = aGroupSummary;
    }

    /**
     * @return the group rooms
     */
    public GroupRooms getGroupRooms() {
        return mRooms;
    }

    /**
     * Update the group rooms
     *
     * @param aGroupRooms the new group rooms
     */
    public void setGroupRooms(GroupRooms aGroupRooms) {
        mRooms = aGroupRooms;
    }

    /**
     * @return the group users
     */
    public GroupUsers getGroupUsers() {
        return mUsers;
    }

    /**
     * Update the group users
     *
     * @param aGroupUsers the group users
     */
    public void setGroupUsers(GroupUsers aGroupUsers) {
        mUsers = aGroupUsers;
    }

    /**
     * @return the invited group users
     */
    public GroupUsers getInvitedGroupUsers() {
        return mInvitedUsers;
    }

    /**
     * Update the invited group users
     *
     * @param aGroupUsers the group users
     */
    public void setInvitedGroupUsers(GroupUsers aGroupUsers) {
        mInvitedUsers = aGroupUsers;
    }

    /**
     * Update the membership
     *
     * @param membership the new membership
     */
    public void setMembership(String membership) {
        mMembership = membership;
    }

    /**
     * @return the membership
     */
    public String getMembership() {
        return mMembership;
    }

    /**
     * @return the inviter
     */
    public String getInviter() {
        return mInviter;
    }

    /**
     * Update the inviter.
     *
     * @param inviter the inviter.
     */
    public void setInviter(String inviter) {
        mInviter = inviter;
    }
}
