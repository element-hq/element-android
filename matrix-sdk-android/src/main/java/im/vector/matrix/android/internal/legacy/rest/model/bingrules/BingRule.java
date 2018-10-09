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
package im.vector.matrix.android.internal.legacy.rest.model.bingrules;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingRule {
    private static final String LOG_TAG = BingRule.class.getSimpleName();

    public static final String RULE_ID_DISABLE_ALL = ".m.rule.master";
    public static final String RULE_ID_CONTAIN_USER_NAME = ".m.rule.contains_user_name";
    public static final String RULE_ID_CONTAIN_DISPLAY_NAME = ".m.rule.contains_display_name";
    public static final String RULE_ID_ONE_TO_ONE_ROOM = ".m.rule.room_one_to_one";
    public static final String RULE_ID_INVITE_ME = ".m.rule.invite_for_me";
    public static final String RULE_ID_PEOPLE_JOIN_LEAVE = ".m.rule.member_event";
    public static final String RULE_ID_CALL = ".m.rule.call";
    public static final String RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS = ".m.rule.suppress_notices";
    public static final String RULE_ID_ALL_OTHER_MESSAGES_ROOMS = ".m.rule.message";
    public static final String RULE_ID_FALLBACK = ".m.rule.fallback";

    public static final String ACTION_NOTIFY = "notify";
    public static final String ACTION_DONT_NOTIFY = "dont_notify";
    public static final String ACTION_COALESCE = "coalesce";

    public static final String ACTION_SET_TWEAK_SOUND_VALUE = "sound";
    public static final String ACTION_SET_TWEAK_HIGHLIGHT_VALUE = "highlight";

    public static final String ACTION_PARAMETER_SET_TWEAK = "set_tweak";
    public static final String ACTION_PARAMETER_VALUE = "value";

    public static final String ACTION_VALUE_DEFAULT = "default";
    public static final String ACTION_VALUE_RING = "ring";

    public static final String KIND_OVERRIDE = "override";
    public static final String KIND_CONTENT = "content";
    public static final String KIND_ROOM = "room";
    public static final String KIND_SENDER = "sender";
    public static final String KIND_UNDERRIDE = "underride";

    public String ruleId = null;
    public List<Condition> conditions = null;
    // Object is either String or Map<String, String>
    public List<Object> actions = null;
    @SerializedName("default")
    public boolean isDefault = false;

    @SerializedName("enabled")
    public boolean isEnabled = true;

    public String kind = null;

    public BingRule(boolean isDefaultValue) {
        isDefault = isDefaultValue;
    }

    public BingRule() {
        isDefault = false;
    }

    @Override
    public String toString() {
        return "BingRule{" +
                "ruleId='" + ruleId + '\'' +
                ", conditions=" + conditions +
                ", actions=" + actions +
                ", isDefault=" + isDefault +
                ", isEnabled=" + isEnabled +
                ", kind='" + kind + '\'' +
                '}';
    }

    /**
     * Convert BingRule to a JsonElement.
     * It seems that "conditions" name triggers conversion issues.
     *
     * @return the JsonElement
     */
    public JsonElement toJsonElement() {
        JsonObject jsonObject = JsonUtils.getGson(false).toJsonTree(this).getAsJsonObject();

        if (null != conditions) {
            jsonObject.add("conditions", JsonUtils.getGson(false).toJsonTree(conditions));
        }

        return jsonObject;
    }

    /**
     * Bing rule creator
     *
     * @param ruleKind  the rule kind
     * @param aPattern  the pattern to check the condition
     * @param notify    true to notify
     * @param highlight true to highlight
     * @param sound     true to play sound
     */
    public BingRule(String ruleKind, String aPattern, Boolean notify, Boolean highlight, boolean sound) {
        //
        ruleId = aPattern;
        isEnabled = true;
        isDefault = false;
        kind = ruleKind;
        conditions = null;

        actions = new ArrayList<>();

        if (null != notify) {
            setNotify(notify);
        }
        
        if (null != highlight) {
            setHighlight(highlight);
        }

        if (sound) {
            setNotificationSound();
        }
    }

    /**
     * Build a bing rule from another one.
     *
     * @param otherRule the other rule
     */
    public BingRule(BingRule otherRule) {
        ruleId = otherRule.ruleId;

        if (null != otherRule.conditions) {
            conditions = new ArrayList<>(otherRule.conditions);
        }

        if (null != otherRule.actions) {
            actions = new ArrayList<>(otherRule.actions);
        }

        isDefault = otherRule.isDefault;
        isEnabled = otherRule.isEnabled;
        kind = otherRule.kind;
    }

    /**
     * Add a condition to the rule.
     *
     * @param condition the condition to add.
     */
    public void addCondition(Condition condition) {
        if (null == conditions) {
            conditions = new ArrayList<>();
        }
        conditions.add(condition);
    }

    /**
     * Search an action map from its tweak.
     *
     * @param tweak the tweak name.
     * @return the action map. null if not found.
     */
    public Map<String, Object> getActionMap(String tweak) {
        if ((null != actions) && !TextUtils.isEmpty(tweak)) {
            for (Object action : actions) {
                if (action instanceof Map) {
                    try {
                        Map<String, Object> actionMap = ((Map<String, Object>) action);

                        if (TextUtils.equals((String) actionMap.get(ACTION_PARAMETER_SET_TWEAK), tweak)) {
                            return actionMap;
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## getActionMap() : " + e.getMessage(), e);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if the sound type is the default notification sound.
     *
     * @param sound the sound name.
     * @return true if the sound is the default notification sound.
     */
    public static boolean isDefaultNotificationSound(String sound) {
        return ACTION_VALUE_DEFAULT.equals(sound);
    }

    /**
     * Check if the sound type is the call ring.
     *
     * @param sound the sound name.
     * @return true if the sound is the call ring.
     */
    public static boolean isCallRingNotificationSound(String sound) {
        return ACTION_VALUE_RING.equals(sound);
    }

    /**
     * @return the notification sound (null if it is not defined)
     */
    public String getNotificationSound() {
        String sound = null;
        Map<String, Object> actionMap = getActionMap(ACTION_SET_TWEAK_SOUND_VALUE);

        if ((null != actionMap) && actionMap.containsKey(ACTION_PARAMETER_VALUE)) {
            sound = (String) actionMap.get(ACTION_PARAMETER_VALUE);
        }

        return sound;
    }

    /**
     * Add the default notification sound.
     */
    public void setNotificationSound() {
        setNotificationSound(ACTION_VALUE_DEFAULT);
    }

    /**
     * Set the notification sound
     *
     * @param sound notification sound
     */
    public void setNotificationSound(String sound) {
        removeNotificationSound();

        if (!TextUtils.isEmpty(sound)) {
            Map<String, String> actionMap = new HashMap<>();
            actionMap.put(ACTION_PARAMETER_SET_TWEAK, ACTION_SET_TWEAK_SOUND_VALUE);
            actionMap.put(ACTION_PARAMETER_VALUE, sound);
            actions.add(actionMap);
        }
    }

    /**
     * Remove the notification sound
     */
    public void removeNotificationSound() {
        Map<String, Object> actionMap = getActionMap(ACTION_SET_TWEAK_SOUND_VALUE);

        if (null != actionMap) {
            actions.remove(actionMap);
        }
    }

    /**
     * Set the highlight status.
     *
     * @param highlight the highlight status
     */
    public void setHighlight(boolean highlight) {
        Map<String, Object> actionMap = getActionMap(ACTION_SET_TWEAK_HIGHLIGHT_VALUE);

        if (null == actionMap) {
            actionMap = new HashMap<>();
            actionMap.put(ACTION_PARAMETER_SET_TWEAK, ACTION_SET_TWEAK_HIGHLIGHT_VALUE);
            actions.add(actionMap);
        }

        if (highlight) {
            actionMap.remove(ACTION_PARAMETER_VALUE);
        } else {
            actionMap.put(ACTION_PARAMETER_VALUE, false);
        }
    }

    /**
     * Return true if the rule should highlight the event.
     *
     * @return true if the rule should play sound
     */
    public boolean shouldHighlight() {
        boolean shouldHighlight = false;

        Map<String, Object> actionMap = getActionMap(ACTION_SET_TWEAK_HIGHLIGHT_VALUE);

        if (null != actionMap) {
            // default behaviour
            shouldHighlight = true;

            if (actionMap.containsKey(ACTION_PARAMETER_VALUE)) {
                Object valueAsVoid = actionMap.get(ACTION_PARAMETER_VALUE);

                if (valueAsVoid instanceof Boolean) {
                    shouldHighlight = (boolean) valueAsVoid;
                } else if (valueAsVoid instanceof String) {
                    shouldHighlight = TextUtils.equals((String)valueAsVoid, "true");
                } else {
                    Log.e(LOG_TAG, "## shouldHighlight() : unexpected type " + valueAsVoid);
                }
            }
        }

        return shouldHighlight;
    }

    /**
     * Set the notification status.
     *
     * @param notify true to notify
     */
    public void setNotify(boolean notify) {
        if (notify) {
            actions.remove(ACTION_DONT_NOTIFY);

            if (!actions.contains(ACTION_NOTIFY)) {
                actions.add(ACTION_NOTIFY);
            }
        } else {
            actions.remove(ACTION_NOTIFY);

            if (!actions.contains(ACTION_DONT_NOTIFY)) {
                actions.add(ACTION_DONT_NOTIFY);
            }
        }
    }

    /**
     * Return true if the rule should highlight the event.
     *
     * @return true if the rule should play sound
     */
    public boolean shouldNotify() {
        return actions.contains(ACTION_NOTIFY);
    }

    /**
     * Return true if the rule should not highlight the event.
     *
     * @return true if the rule should not play sound
     */
    public boolean shouldNotNotify() {
        return actions.contains(ACTION_DONT_NOTIFY);
    }
}
