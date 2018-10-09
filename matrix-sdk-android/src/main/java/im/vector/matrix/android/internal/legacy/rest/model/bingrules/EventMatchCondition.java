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

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import im.vector.matrix.android.internal.legacy.rest.model.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class EventMatchCondition extends Condition {

    public String key;
    public String pattern;

    private static Map<String, Pattern> mPatternByRule = null;

    public EventMatchCondition() {
        kind = Condition.KIND_EVENT_MATCH;
    }

    @Override
    public String toString() {
        return "EventMatchCondition{" + "key='" + key + ", pattern=" + pattern + '}';
    }

    /**
     * Returns whether the given event satisfies the condition.
     *
     * @param event the event
     * @return true if the event satisfies the condition
     */
    public boolean isSatisfied(Event event) {
        String fieldVal = null;

        // some information are in the decrypted event (like type)
        if (event.isEncrypted() && (null != event.getClearEvent())) {
            JsonObject eventJson = event.getClearEvent().toJsonObject();
            fieldVal = extractField(eventJson, key);
        }

        if (TextUtils.isEmpty(fieldVal)) {
            JsonObject eventJson = event.toJsonObject();
            fieldVal = extractField(eventJson, key);
        }

        if (TextUtils.isEmpty(fieldVal)) {
            return false;
        }

        if (TextUtils.equals(pattern, fieldVal)) {
            return true;
        }

        if (null == mPatternByRule) {
            mPatternByRule = new HashMap<>();
        }

        Pattern patternEx = mPatternByRule.get(pattern);

        if (null == patternEx) {
            patternEx = Pattern.compile(globToRegex(pattern), Pattern.CASE_INSENSITIVE);
            mPatternByRule.put(pattern, patternEx);
        }

        return patternEx.matcher(fieldVal).matches();
    }

    private String extractField(JsonObject jsonObject, String fieldPath) {
        String[] fieldParts = fieldPath.split("\\.");
        JsonElement jsonElement = null;
        for (String field : fieldParts) {
            jsonElement = jsonObject.get(field);
            if (jsonElement == null) {
                return null;
            }
            if (jsonElement.isJsonObject()) {
                jsonObject = (JsonObject) jsonElement;
            }
        }
        return (jsonElement == null) ? null : jsonElement.getAsString();
    }

    private String globToRegex(String glob) {
        String res = glob.replace("*", ".*").replace("?", ".");

        // If no special characters were found (detected here by no replacements having been made),
        // add asterisks and boundaries to both sides
        if (res.equals(glob)) {
            res = "(^|.*\\W)" + res + "($|\\W.*)";
        }
        return res;
    }
}
