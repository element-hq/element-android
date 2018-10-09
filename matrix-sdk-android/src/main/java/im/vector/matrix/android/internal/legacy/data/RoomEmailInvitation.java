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

package im.vector.matrix.android.internal.legacy.data;

import java.util.Map;

/**
 * Class representing the email invitation parameters
 */
public class RoomEmailInvitation {

    // the email invitation parameters
    // earch parameter can be null
    public String email;
    public String signUrl;
    public String roomName;
    public String roomAvatarUrl;
    public String inviterName;
    public String guestAccessToken;
    public String guestUserId;

    // the constructor
    public RoomEmailInvitation(Map<String, String> parameters) {

        if (null != parameters) {
            email = parameters.get("email");
            signUrl = parameters.get("signurl");
            roomName = parameters.get("room_name");
            roomAvatarUrl = parameters.get("room_avatar_url");
            inviterName = parameters.get("inviter_name");
            guestAccessToken = parameters.get("guestAccessToken");
            guestUserId = parameters.get("guest_user_id");
        }
    }
}
