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
package im.vector.matrix.android.internal.legacy.util;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.RoomDirectoryVisibility;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.BingRule;

import java.util.regex.Pattern;

/**
 * Utility methods for events.
 */
public class EventUtils {
    private static final String LOG_TAG = EventUtils.class.getSimpleName();

    /**
     * Whether the given event should be highlighted in its chat room.
     *
     * @param session the session.
     * @param event   the event
     * @return whether the event is important and should be highlighted
     */
    public static boolean shouldHighlight(MXSession session, Event event) {
        // sanity check
        if ((null == session) || (null == event)) {
            return false;
        }

        boolean res = false;

        // search if the event fulfills a rule
        BingRule rule = session.fulfillRule(event);

        if (null != rule) {
            res = rule.shouldHighlight();

            if (res) {
                Log.d(LOG_TAG, "## shouldHighlight() : the event " + event.roomId + "/" + event.eventId + " is higlighted by " + rule);
            }
        }

        return res;
    }

    /**
     * Whether the given event should trigger a notification.
     *
     * @param session      the current matrix session
     * @param event        the event
     * @param activeRoomID the RoomID of disaplyed roomActivity
     * @return true if the event should trigger a notification
     */
    public static boolean shouldNotify(MXSession session, Event event, String activeRoomID) {
        if ((null == event) || (null == session)) {
            Log.e(LOG_TAG, "shouldNotify invalid params");
            return false;
        }

        // Only room events trigger notifications
        if (null == event.roomId) {
            Log.e(LOG_TAG, "shouldNotify null room ID");
            return false;
        }

        if (null == event.getSender()) {
            Log.e(LOG_TAG, "shouldNotify null room ID");
            return false;
        }

        // No notification if the user is currently viewing the room
        if (TextUtils.equals(event.roomId, activeRoomID)) {
            return false;
        }

        if (shouldHighlight(session, event)) {
            return true;
        }

        Room room = session.getDataHandler().getRoom(event.roomId);
        return RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PRIVATE.equals(room.getVisibility())
                && !TextUtils.equals(event.getSender(), session.getCredentials().userId);
    }

    /**
     * Returns whether a string contains an occurrence of another, as a standalone word, regardless of case.
     *
     * @param subString  the string to search for
     * @param longString the string to search in
     * @return whether a match was found
     */
    public static boolean caseInsensitiveFind(String subString, String longString) {
        // add sanity checks
        if (TextUtils.isEmpty(subString) || TextUtils.isEmpty(longString)) {
            return false;
        }

        boolean res = false;

        try {
            Pattern pattern = Pattern.compile("(\\W|^)" + Pattern.quote(subString) + "(\\W|$)", Pattern.CASE_INSENSITIVE);
            res = pattern.matcher(longString).find();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## caseInsensitiveFind() : failed", e);
        }

        return res;
    }
}
