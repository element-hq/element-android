/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.pushrules

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import timber.log.Timber

class ContainsDisplayNameCondition : Condition(Kind.ContainsDisplayName) {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveContainsDisplayNameCondition(event, this)
    }

    override fun technicalDescription(): String {
        return "User is mentioned"
    }

    fun isSatisfied(event: Event, displayName: String): Boolean {
        val message = when (event.type) {
            EventType.MESSAGE -> {
                event.content.toModel<MessageContent>()
            }
            // TODO the spec says:
            // Matches any message whose content is unencrypted and contains the user's current display name
            // EventType.ENCRYPTED -> {
            //     event.root.getClearContent()?.toModel<MessageContent>()
            // }
            else              -> null
        } ?: return false

        return caseInsensitiveFind(displayName, message.body)
    }

    companion object {
        /**
         * Returns whether a string contains an occurrence of another, as a standalone word, regardless of case.
         *
         * @param subString  the string to search for
         * @param longString the string to search in
         * @return whether a match was found
         */
        fun caseInsensitiveFind(subString: String, longString: String): Boolean {
            // add sanity checks
            if (subString.isEmpty() || longString.isEmpty()) {
                return false
            }

            try {
                val regex = Regex("(\\W|^)" + Regex.escape(subString) + "(\\W|$)", RegexOption.IGNORE_CASE)
                return regex.containsMatchIn(longString)
            } catch (e: Exception) {
                Timber.e(e, "## caseInsensitiveFind() : failed")
            }

            return false
        }
    }
}
