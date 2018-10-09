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
package im.vector.matrix.android.internal.legacy.rest.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import im.vector.matrix.android.internal.legacy.util.ContentManager;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;

/**
 * Class representing a room member: a user with membership information.
 */
public class RoomMember implements Externalizable {
    private static final String LOG_TAG = RoomMember.class.getSimpleName();

    public static final String MEMBERSHIP_JOIN = "join";
    public static final String MEMBERSHIP_INVITE = "invite";
    public static final String MEMBERSHIP_LEAVE = "leave";
    public static final String MEMBERSHIP_BAN = "ban";

    // not supported by the server sync response by computed from the room state events
    public static final String MEMBERSHIP_KICK = "kick";

    public String displayname;
    public String avatarUrl;
    public String membership;
    public Invite thirdPartyInvite;

    // tells that the inviter starts a direct chat room
    @SerializedName("is_direct")
    public Boolean isDirect;

    private String userId = null;
    // timestamp of the event which has created this member
    private long mOriginServerTs = -1;

    // the event used to build the room member
    private String mOriginalEventId = null;

    // kick / ban reason
    public String reason;
    // user which banned or kicked this member
    public String mSender;

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        if (input.readBoolean()) {
            displayname = input.readUTF();
        }

        if (input.readBoolean()) {
            avatarUrl = input.readUTF();
        }

        if (input.readBoolean()) {
            membership = input.readUTF();
        }

        if (input.readBoolean()) {
            thirdPartyInvite = (Invite) input.readObject();
        }

        if (input.readBoolean()) {
            isDirect = input.readBoolean();
        }

        if (input.readBoolean()) {
            userId = input.readUTF();
        }

        mOriginServerTs = input.readLong();

        if (input.readBoolean()) {
            mOriginalEventId = input.readUTF();
        }

        if (input.readBoolean()) {
            reason = input.readUTF();
        }

        if (input.readBoolean()) {
            mSender = input.readUTF();
        }
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeBoolean(null != displayname);
        if (null != displayname) {
            output.writeUTF(displayname);
        }

        output.writeBoolean(null != avatarUrl);
        if (null != avatarUrl) {
            output.writeUTF(avatarUrl);
        }

        output.writeBoolean(null != membership);
        if (null != membership) {
            output.writeUTF(membership);
        }

        output.writeBoolean(null != thirdPartyInvite);
        if (null != thirdPartyInvite) {
            output.writeObject(thirdPartyInvite);
        }

        output.writeBoolean(null != isDirect);
        if (null != isDirect) {
            output.writeBoolean(isDirect);
        }

        output.writeBoolean(null != userId);
        if (null != userId) {
            output.writeUTF(userId);
        }

        output.writeLong(mOriginServerTs);

        output.writeBoolean(null != mOriginalEventId);
        if (null != mOriginalEventId) {
            output.writeUTF(mOriginalEventId);
        }

        output.writeBoolean(null != reason);
        if (null != reason) {
            output.writeUTF(reason);
        }

        output.writeBoolean(null != mSender);
        if (null != mSender) {
            output.writeUTF(mSender);
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setOriginServerTs(long aTs) {
        mOriginServerTs = aTs;
    }

    public long getOriginServerTs() {
        return mOriginServerTs;
    }

    public void setOriginalEventId(String eventId) {
        mOriginalEventId = eventId;
    }

    public String getOriginalEventId() {
        return mOriginalEventId;
    }

    public String getAvatarUrl() {
        // allow only url which starts with mxc://
        if ((null != avatarUrl) && !avatarUrl.toLowerCase().startsWith(ContentManager.MATRIX_CONTENT_URI_SCHEME)) {
            Log.e(LOG_TAG, "## getAvatarUrl() : the member " + userId + " has an invalid avatar url " + avatarUrl);
            return null;
        }

        return avatarUrl;
    }

    public void setAvatarUrl(String anAvatarUrl) {
        avatarUrl = anAvatarUrl;
    }

    public String getThirdPartyInviteToken() {
        if ((null != thirdPartyInvite) && (null != thirdPartyInvite.signed)) {
            return thirdPartyInvite.signed.token;
        }

        return null;
    }

    // Comparator to order members alphabetically
    public static Comparator<RoomMember> alphaComparator = new Comparator<RoomMember>() {
        @Override
        public int compare(RoomMember member1, RoomMember member2) {
            String lhs = member1.getName();
            String rhs = member2.getName();

            if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            }
            if (lhs.startsWith("@")) {
                lhs = lhs.substring(1);
            }
            if (rhs.startsWith("@")) {
                rhs = rhs.substring(1);
            }
            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    };

    /**
     * Test if a room member fields matches with a pattern.
     * The check is done with the displayname and the userId.
     *
     * @param aPattern the pattern to search.
     * @return true if it matches.
     */
    public boolean matchWithPattern(String aPattern) {
        if (TextUtils.isEmpty(aPattern) || TextUtils.isEmpty(aPattern.trim())) {
            return false;
        }

        boolean res = false;

        if (!TextUtils.isEmpty(displayname)) {
            res = (displayname.toLowerCase().indexOf(aPattern) >= 0);
        }

        if (!res && !TextUtils.isEmpty(userId)) {
            res = (userId.toLowerCase().indexOf(aPattern) >= 0);
        }

        return res;
    }

    /**
     * Test if a room member matches with a reg ex.
     * The check is done with the displayname and the userId.
     *
     * @param aRegEx the reg ex
     * @return true if it matches.
     */
    public boolean matchWithRegEx(String aRegEx) {
        if (TextUtils.isEmpty(aRegEx)) {
            return false;
        }

        boolean res = false;

        if (!TextUtils.isEmpty(displayname)) {
            res = displayname.matches(aRegEx);
        }

        if (!res && !TextUtils.isEmpty(userId)) {
            res = userId.matches(aRegEx);
        }

        return res;
    }

    /**
     * Compare two members.
     * The members are equals if each field have the same value.
     *
     * @param otherMember the member to compare.
     * @return true if they define the same member.
     */
    public boolean equals(RoomMember otherMember) {
        // compare to null
        if (null == otherMember) {
            return false;
        }

        // compare display name
        boolean isEqual = TextUtils.equals(displayname, otherMember.displayname);

        if (isEqual) {
            isEqual = TextUtils.equals(avatarUrl, otherMember.avatarUrl);
        }

        if (isEqual) {
            isEqual = TextUtils.equals(membership, otherMember.membership);
        }

        if (isEqual) {
            isEqual = TextUtils.equals(userId, otherMember.userId);
        }

        return isEqual;
    }

    public String getName() {
        if (displayname != null) {
            return displayname;
        }
        if (userId != null) {
            return userId;
        }
        return null;
    }

    /**
     * Prune the room member data as we would have done with its original state event.
     */
    public void prune() {
        // Redact redactable data
        displayname = null;
        avatarUrl = null;
        reason = null;

        // Note: if we had access to the original event content, we should store
        // the `redacted_because` of the redaction event in it.
    }

    public RoomMember deepCopy() {
        RoomMember copy = new RoomMember();
        copy.displayname = displayname;
        copy.avatarUrl = avatarUrl;
        copy.membership = membership;
        copy.userId = userId;
        copy.mOriginalEventId = mOriginalEventId;
        copy.mSender = mSender;
        copy.reason = reason;
        return copy;
    }

    /**
     * @return true if the user has been banned or kicked
     */
    public boolean kickedOrBanned() {
        return TextUtils.equals(membership, MEMBERSHIP_KICK) || TextUtils.equals(membership, MEMBERSHIP_BAN);
    }
}
