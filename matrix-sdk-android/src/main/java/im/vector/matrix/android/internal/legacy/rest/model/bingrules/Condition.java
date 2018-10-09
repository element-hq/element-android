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
package im.vector.matrix.android.internal.legacy.rest.model.bingrules;


public class Condition {
    // defined in the push rules spec
    // https://matrix.org/docs/spec/client_server/r0.3.0.html#push-rules

    /* 'key': The dot-separated field of the event to match, eg. content.body
       'pattern': The glob-style pattern to match against. Patterns with no special glob characters should be treated as having asterisks prepended
        and appended when testing the condition.*/
    public static final String KIND_EVENT_MATCH = "event_match";

    /* 'profile_tag': The profile_tag to match with.*/
    public static final String KIND_PROFILE_TAG = "profile_tag";

    /* no parameter */
    public static final String KIND_CONTAINS_DISPLAY_NAME = "contains_display_name";

    /* 'is': A decimal integer optionally prefixed by one of, '==', '<', '>', '>=' or '<='.
        A prefix of '<' matches rooms where the member count is strictly less than the given number and so forth. If no prefix is present, this matches
         rooms where the member count is exactly equal to the given number (ie. the same as '==').
     */
    public static final String KIND_ROOM_MEMBER_COUNT = "room_member_count";

    /* */
    public static final String KIND_DEVICE = "device";

    public static final String KIND_SENDER_NOTIFICATION_PERMISSION = "sender_notification_permission";

    public static final String KIND_UNKNOWN = "unknown_condition";

    public String kind;
}
