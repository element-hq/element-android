/*
 * Copyright 2014 OpenMarket Ltd
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

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig;
import im.vector.matrix.android.api.auth.data.Credentials;
import im.vector.matrix.android.internal.legacy.MXPatterns;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.rest.model.pid.Invite3Pid;
import im.vector.matrix.android.internal.legacy.rest.model.pid.ThreePid;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;

public class CreateRoomParams {

    public static final String PRESET_PRIVATE_CHAT = "private_chat";
    public static final String PRESET_PUBLIC_CHAT = "public_chat";
    public static final String PRESET_TRUSTED_PRIVATE_CHAT = "trusted_private_chat";

    /**
     * A public visibility indicates that the room will be shown in the published room list.
     * A private visibility will hide the room from the published room list.
     * Rooms default to private visibility if this key is not included.
     * NB: This should not be confused with join_rules which also uses the word public. One of: ["public", "private"]
     */
    public String visibility;

    /**
     * The desired room alias local part. If this is included, a room alias will be created and mapped to the newly created room.
     * The alias will belong on the same homeserver which created the room.
     * For example, if this was set to "foo" and sent to the homeserver "example.com" the complete room alias would be #foo:example.com.
     */
    @SerializedName("room_alias_name")
    public String roomAliasName;

    /**
     * If this is included, an m.room.name event will be sent into the room to indicate the name of the room.
     * See Room Events for more information on m.room.name.
     */
    public String name;

    /**
     * If this is included, an m.room.topic event will be sent into the room to indicate the topic for the room.
     * See Room Events for more information on m.room.topic.
     */
    public String topic;

    /**
     * A list of user IDs to invite to the room.
     * This will tell the server to invite everyone in the list to the newly created room.
     */
    @SerializedName("invite")
    public List<String> invitedUserIds;

    /**
     * A list of objects representing third party IDs to invite into the room.
     */
    @SerializedName("invite_3pid")
    public List<Invite3Pid> invite3pids;

    /**
     * Extra keys to be added to the content of the m.room.create.
     * The server will clobber the following keys: creator.
     * Future versions of the specification may allow the server to clobber other keys.
     */
    public Object creation_content;

    /**
     * A list of state events to set in the new room.
     * This allows the user to override the default state events set in the new room.
     * The expected format of the state events are an object with type, state_key and content keys set.
     * Takes precedence over events set by presets, but gets overriden by name and topic keys.
     */
    @SerializedName("initial_state")
    public List<Event> initialStates;

    /**
     * Convenience parameter for setting various default state events based on a preset. Must be either:
     * private_chat => join_rules is set to invite. history_visibility is set to shared.
     * trusted_private_chat => join_rules is set to invite. history_visibility is set to shared. All invitees are given the same power level as the
     * room creator.
     * public_chat: => join_rules is set to public. history_visibility is set to shared. One of: ["private_chat", "public_chat", "trusted_private_chat"]
     */
    public String preset;

    /**
     * This flag makes the server set the is_direct flag on the m.room.member events sent to the users in invite and invite_3pid.
     * See Direct Messaging for more information.
     */
    @SerializedName("is_direct")
    public Boolean isDirect;

    /**
     * Add the crypto algorithm to the room creation parameters.
     *
     * @param algorithm the algorithm
     */
    public void addCryptoAlgorithm(String algorithm) {
        if (!TextUtils.isEmpty(algorithm)) {
            Event algoEvent = new Event();
            algoEvent.type = Event.EVENT_TYPE_MESSAGE_ENCRYPTION;

            Map<String, String> contentMap = new HashMap<>();
            contentMap.put("algorithm", algorithm);
            algoEvent.content = JsonUtils.getGson(false).toJsonTree(contentMap);

            if (null == initialStates) {
                initialStates = Arrays.asList(algoEvent);
            } else {
                initialStates.add(algoEvent);
            }
        }
    }

    /**
     * Force the history visibility in the room creation parameters.
     *
     * @param historyVisibility the expected history visibility, set null to remove any existing value.
     *                          see {@link RoomState#HISTORY_VISIBILITY_INVITED},
     *                          {@link RoomState#HISTORY_VISIBILITY_JOINED},
     *                          {@link RoomState#HISTORY_VISIBILITY_SHARED},
     *                          {@link RoomState#HISTORY_VISIBILITY_WORLD_READABLE}
     */
    public void setHistoryVisibility(@Nullable String historyVisibility) {
        if (!TextUtils.isEmpty(historyVisibility)) {
            Event historyVisibilityEvent = new Event();
            historyVisibilityEvent.type = Event.EVENT_TYPE_STATE_HISTORY_VISIBILITY;

            Map<String, String> contentMap = new HashMap<>();
            contentMap.put("history_visibility", historyVisibility);
            historyVisibilityEvent.content = JsonUtils.getGson(false).toJsonTree(contentMap);

            if (null == initialStates) {
                initialStates = Arrays.asList(historyVisibilityEvent);
            } else {
                initialStates.add(historyVisibilityEvent);
            }
        } else if (!initialStates.isEmpty()) {
            final List<Event> newInitialStates = new ArrayList<>();
            for (Event event : initialStates) {
                if (!event.type.equals(Event.EVENT_TYPE_STATE_HISTORY_VISIBILITY)) {
                    newInitialStates.add(event);
                }
            }
            initialStates = newInitialStates;
        }
    }

    /**
     * Mark as a direct message room.
     */
    public void setDirectMessage() {
        preset = CreateRoomParams.PRESET_TRUSTED_PRIVATE_CHAT;
        isDirect = true;
    }

    /**
     * @return the invite count
     */
    private int getInviteCount() {
        return (null == invitedUserIds) ? 0 : invitedUserIds.size();
    }

    /**
     * @return the pid invite count
     */
    private int getInvite3PidCount() {
        return (null == invite3pids) ? 0 : invite3pids.size();
    }

    /**
     * Tells if the created room can be a direct chat one.
     *
     * @return true if it is a direct chat
     */
    public boolean isDirect() {
        return TextUtils.equals(preset, CreateRoomParams.PRESET_TRUSTED_PRIVATE_CHAT)
                && (null != isDirect)
                && isDirect
                && (1 == getInviteCount() || (1 == getInvite3PidCount()));
    }

    /**
     * @return the first invited user id
     */
    public String getFirstInvitedUserId() {
        if (0 != getInviteCount()) {
            return invitedUserIds.get(0);
        }

        if (0 != getInvite3PidCount()) {
            return invite3pids.get(0).address;
        }

        return null;
    }

    /**
     * Add some ids to the room creation
     * ids might be a matrix id or an email address.
     *
     * @param ids the participant ids to add.
     */
    public void addParticipantIds(HomeServerConnectionConfig homeServerConnectionConfig, Credentials credentials, List<String> ids) {
        for (String id : ids) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches()) {
                if (null == invite3pids) {
                    invite3pids = new ArrayList<>();
                }

                Invite3Pid pid = new Invite3Pid();
                pid.id_server = homeServerConnectionConfig.getIdentityServerUri().getHost();
                pid.medium = ThreePid.MEDIUM_EMAIL;
                pid.address = id;

                invite3pids.add(pid);
            } else if (MXPatterns.isUserId(id)) {
                // do not invite oneself
                if (!TextUtils.equals(credentials.getUserId(), id)) {
                    if (null == invitedUserIds) {
                        invitedUserIds = new ArrayList<>();
                    }

                    invitedUserIds.add(id);
                }

            } // TODO add phonenumbers when it will be available
        }
    }
}
