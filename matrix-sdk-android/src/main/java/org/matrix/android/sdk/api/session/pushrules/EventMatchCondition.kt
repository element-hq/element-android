/*
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
package org.matrix.android.sdk.api.session.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.simpleGlobToRegExp
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
) : Condition {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveEventMatchCondition(event, this)
    }

    override fun technicalDescription() = "'$key' matches '$pattern'"

    fun isSatisfied(event: Event): Boolean {
        val rawJson: Map<*, *> = (MoshiProvider.providesMoshi().adapter(Event::class.java).toJsonValue(event) as? Map<*, *>)
                ?.let { rawJson ->
                    val decryptedRawJson = event.mxDecryptionResult?.payload
                    if (decryptedRawJson != null) {
                        rawJson
                                .toMutableMap()
                                .apply { putAll(decryptedRawJson) }
                    } else {
                        rawJson
                    }
                }
                ?: return false
        val value = extractField(rawJson, key) ?: return false

        // The match is performed case-insensitively, and must match the entire value of
        // the event field given by `key` (though see below regarding `content.body`). The
        // exact meaning of "case insensitive" is defined by the implementation of the
        // homeserver.
        //
        // As a special case, if `key` is `content.body`, then `pattern` must instead
        // match any substring of the value of the property which starts and ends at a
        // word boundary.
        return try {
            if (key == "content.body") {
                val modPattern = if (pattern.startsWith("*") && pattern.endsWith("*")) {
                    // Regex.containsMatchIn() is way faster without leading and trailing
                    // stars, that don't make any difference for the evaluation result
                    pattern.removePrefix("*").removeSuffix("*").simpleGlobToRegExp()
                } else {
                    "(\\W|^)" + pattern.simpleGlobToRegExp() + "(\\W|$)"
                }
                val regex = Regex(modPattern, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                regex.containsMatchIn(value)
            } else {
                val regex = Regex(pattern.simpleGlobToRegExp(), setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                regex.matches(value)
            }
        } catch (e: Throwable) {
            // e.g PatternSyntaxException
            Timber.e(e, "Failed to evaluate push condition")
            false
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
}
