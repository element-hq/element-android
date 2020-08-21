/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

class EventMatchCondition(
        /**
         * The dot-separated field of the event to match, e.g. content.body
         */
        val key: String,
        /**
         * The glob-style pattern to match against. Patterns with no special glob characters should
         * be treated as having asterisks prepended and appended when testing the condition.
         */
        val pattern: String
) : Condition(Kind.EventMatch) {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveEventMatchCondition(event, this)
    }

    override fun technicalDescription(): String {
        return "'$key' Matches '$pattern'"
    }

    fun isSatisfied(event: Event): Boolean {
        // TODO encrypted events?
        val rawJson = MoshiProvider.providesMoshi().adapter(Event::class.java).toJsonValue(event) as? Map<*, *>
                ?: return false
        val value = extractField(rawJson, key) ?: return false

        // Patterns with no special glob characters should be treated as having asterisks prepended
        // and appended when testing the condition.
        try {
            val modPattern = if (hasSpecialGlobChar(pattern)) simpleGlobToRegExp(pattern) else simpleGlobToRegExp("*$pattern*")
            val regex = Regex(modPattern, RegexOption.DOT_MATCHES_ALL)
            return regex.containsMatchIn(value)
        } catch (e: Throwable) {
            // e.g PatternSyntaxException
            Timber.e(e, "Failed to evaluate push condition")
            return false
        }
    }

    private fun extractField(jsonObject: Map<*, *>, fieldPath: String): String? {
        val fieldParts = fieldPath.split(".")
        if (fieldParts.isEmpty()) return null

        var jsonElement: Map<*, *> = jsonObject
        fieldParts.forEachIndexed { index, pathSegment ->
            if (index == fieldParts.lastIndex) {
                return jsonElement[pathSegment]?.toString()
            } else {
                val sub = jsonElement[pathSegment] ?: return null
                if (sub is Map<*, *>) {
                    jsonElement = sub
                } else {
                    return null
                }
            }
        }
        return null
    }

    companion object {

        private fun hasSpecialGlobChar(glob: String): Boolean {
            return glob.contains("*") || glob.contains("?")
        }

        // Very simple glob to regexp converter
        private fun simpleGlobToRegExp(glob: String): String {
            var out = "" // "^"
            for (element in glob) {
                when (element) {
                    '*'  -> out += ".*"
                    '?'  -> out += '.'.toString()
                    '.'  -> out += "\\."
                    '\\' -> out += "\\\\"
                    else -> out += element
                }
            }
            out += "" // '$'.toString()
            return out
        }
    }
}
