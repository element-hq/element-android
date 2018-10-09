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
package im.vector.matrix.android.internal.legacy.rest.model.publicroom;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class representing the objects returned by /publicRooms call.
 */
public class PublicRoom {

    public List<String> aliases;

    @SerializedName("canonical_alias")
    public String canonicalAlias;

    public String name;

    // number of members which have joined the room (the members list is not provided)
    @SerializedName("num_joined_members")
    public int numJoinedMembers;

    @SerializedName("room_id")
    public String roomId;

    public String topic;

    // true when the room history is visible (room preview)
    @SerializedName("world_readable")
    public boolean worldReadable;

    // a guest can join the room
    @SerializedName("guest_can_join")
    public boolean guestCanJoin;

    @SerializedName("avatar_url")
    public String avatarUrl;
}
