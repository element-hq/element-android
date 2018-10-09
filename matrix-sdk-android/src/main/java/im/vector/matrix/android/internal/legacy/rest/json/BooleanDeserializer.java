/*
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

package im.vector.matrix.android.internal.legacy.rest.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import im.vector.matrix.android.internal.legacy.util.Log;

import java.lang.reflect.Type;

/**
 * Convenient JsonDeserializer to accept various type of Boolean
 */
public class BooleanDeserializer implements JsonDeserializer<Boolean> {

    private static final String LOG_TAG = BooleanDeserializer.class.getSimpleName();

    private final boolean mCanReturnNull;

    /**
     * Constructor
     *
     * @param canReturnNull true if the deserializer can return null in case of error
     */
    public BooleanDeserializer(boolean canReturnNull) {
        mCanReturnNull = canReturnNull;
    }

    /**
     * @param json    The Json data being deserialized
     * @param typeOfT The type of the Object to deserialize to
     * @param context not used
     * @return true if json is: true, 1, "true" or "1". false for other values. null in other cases.
     * @throws JsonParseException
     */
    @Override
    public Boolean deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

            if (jsonPrimitive.isBoolean()) {
                // Nominal case
                return jsonPrimitive.getAsBoolean();
            } else if (jsonPrimitive.isNumber()) {
                Log.w(LOG_TAG, "Boolean detected as a number");
                return jsonPrimitive.getAsInt() == 1;
            } else if (jsonPrimitive.isString()) {
                Log.w(LOG_TAG, "Boolean detected as a string");

                String jsonPrimitiveString = jsonPrimitive.getAsString();
                return "1".equals(jsonPrimitiveString)
                        || "true".equals(jsonPrimitiveString);
            } else {
                // Should not happen
                Log.e(LOG_TAG, "Unknown primitive");
                if (mCanReturnNull) {
                    return null;
                } else {
                    return false;
                }
            }
        } else if (json.isJsonNull()) {
            if (mCanReturnNull) {
                return null;
            } else {
                Log.w(LOG_TAG, "Boolean is null, but not allowed to return null");
                return false;
            }
        }

        Log.w(LOG_TAG, "Boolean detected as not a primitive type");
        if (mCanReturnNull) {
            return null;
        } else {
            return false;
        }
    }
}
