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

import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Based on FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.
 * toLowerCase() is replaced by toLowerCase(Locale.ENGLISH).
 * In some languages like turkish, toLowerCase does not provide the expected string.
 * e.g _I is not converted to _i.
 */
public class MatrixFieldNamingStrategy implements FieldNamingStrategy {

    /**
     * Converts the field name that uses camel-case define word separation into
     * separate words that are separated by the provided {@code separatorString}.
     */
    private static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }

    /**
     * Translates the field name into its JSON field name representation.
     *
     * @param f the field object that we are translating
     * @return the translated field name.
     * @since 1.3
     */
    public String translateName(Field f) {
        return separateCamelCase(f.getName(), "_").toLowerCase(Locale.ENGLISH);
    }
}
