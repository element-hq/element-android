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
package im.vector.matrix.android.internal.legacy.rest.model;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class PowerLevels implements java.io.Serializable {
    public int ban = 50;
    public int kick = 50;
    public int invite = 50;
    public int redact = 50;

    public int events_default = 0;
    public Map<String, Integer> events = new HashMap<>();

    public int users_default = 0;
    public Map<String, Integer> users = new HashMap<>();

    public int state_default = 50;

    public Map<String, Object> notifications = new HashMap<>();

    public PowerLevels deepCopy() {
        PowerLevels copy = new PowerLevels();
        copy.ban = ban;
        copy.kick = kick;
        copy.invite = invite;
        copy.redact = redact;

        copy.events_default = events_default;
        copy.events = new HashMap<>();
        copy.events.putAll(events);

        copy.users_default = users_default;
        copy.users = new HashMap<>();
        copy.users.putAll(users);

        copy.state_default = state_default;

        copy.notifications = new HashMap<>(notifications);

        return copy;
    }

    /**
     * Returns the user power level of a dedicated user Id
     *
     * @param userId the user id
     * @return the power level
     */
    public int getUserPowerLevel(String userId) {
        // sanity check
        if (!TextUtils.isEmpty(userId)) {
            Integer powerLevel = users.get(userId);
            return (powerLevel != null) ? powerLevel : users_default;
        }

        return users_default;
    }

    /**
     * Updates the user power levels of a dedicated user id
     *
     * @param userId     the user
     * @param powerLevel the new power level
     */
    public void setUserPowerLevel(String userId, int powerLevel) {
        if (null != userId) {
            users.put(userId, Integer.valueOf(powerLevel));
        }
    }

    /**
     * Tell if an user can send an event of type 'eventTypeString'.
     *
     * @param eventTypeString the event type  (in Event.EVENT_TYPE_XXX values)
     * @param userId          the user id
     * @return true if the user can send the event
     */
    public boolean maySendEventOfType(String eventTypeString, String userId) {
        if (!TextUtils.isEmpty(eventTypeString) && !TextUtils.isEmpty(userId)) {
            return getUserPowerLevel(userId) >= minimumPowerLevelForSendingEventAsMessage(eventTypeString);
        }

        return false;
    }

    /**
     * Tells if an user can send a room message.
     *
     * @param userId the user id
     * @return true if the user can send a room message
     */
    public boolean maySendMessage(String userId) {
        return maySendEventOfType(Event.EVENT_TYPE_MESSAGE, userId);
    }

    /**
     * Helper to get the minimum power level the user must have to send an event of the given type
     * as a message.
     *
     * @param eventTypeString the type of event (in Event.EVENT_TYPE_XXX values)
     * @return the required minimum power level.
     */
    public int minimumPowerLevelForSendingEventAsMessage(String eventTypeString) {
        int minimumPowerLevel = events_default;

        if ((null != eventTypeString) && events.containsKey(eventTypeString)) {
            minimumPowerLevel = events.get(eventTypeString);
        }

        return minimumPowerLevel;
    }

    /**
     * Helper to get the minimum power level the user must have to send an event of the given type
     * as a state event.
     *
     * @param eventTypeString the type of event (in Event.EVENT_TYPE_STATE_ values).
     * @return the required minimum power level.
     */
    public int minimumPowerLevelForSendingEventAsStateEvent(String eventTypeString) {
        int minimumPowerLevel = state_default;

        if ((null != eventTypeString) && events.containsKey(eventTypeString)) {
            minimumPowerLevel = events.get(eventTypeString);
        }

        return minimumPowerLevel;
    }


    /**
     * Get the notification level for a dedicated key.
     *
     * @param key the notification key
     * @return the level
     */
    public int notificationLevel(String key) {
        if ((null != key) && notifications.containsKey(key)) {
            Object valAsVoid = notifications.get(key);

            // the first implementation was a string value
            if (valAsVoid instanceof String) {
                return Integer.parseInt((String) valAsVoid);
            } else {
                return (int) valAsVoid;
            }
        }

        return 50;
    }
}