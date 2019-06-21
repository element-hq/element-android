/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.api.pushrules

import android.text.TextUtils
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import java.util.regex.Pattern

class ContainsDisplayNameCondition : Condition(Kind.contains_display_name) {

    override fun isSatisfied(conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveContainsDisplayNameCondition(this)
    }

    override fun technicalDescription(): String {
        return "User is mentioned"
    }

    fun isSatisfied(event: Event, displayName: String): Boolean {
        //TODO the spec says:
        // Matches any message whose content is unencrypted and contains the user's current display name
        var message = when (event.type) {
            EventType.MESSAGE   -> {
                event.content.toModel<MessageContent>()
            }
//            EventType.ENCRYPTED -> {
//                event.root.getClearContent()?.toModel<MessageContent>()
//            }
            else                -> null
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
            if (TextUtils.isEmpty(subString) || TextUtils.isEmpty(longString)) {
                return false
            }

            var res = false

            try {
                val pattern = Pattern.compile("(\\W|^)" + Pattern.quote(subString) + "(\\W|$)", Pattern.CASE_INSENSITIVE)
                res = pattern.matcher(longString).find()
            } catch (e: Exception) {
                Timber.e(e, "## caseInsensitiveFind() : failed")
            }

            return res
        }
    }
}