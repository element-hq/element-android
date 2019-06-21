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
package im.vector.riotredesign.features.notifications

import im.vector.matrix.android.api.pushrules.Action

data class NotificationAction(
        val shouldNotify: Boolean,
        val highlight: Boolean = false,
        val soundName: String? = null
) {
    companion object {
        fun extractFrom(ruleActions: List<Action>): NotificationAction {
            var shouldNotify = false
            var highlight = false
            var sound: String? = null
            ruleActions.forEach {
                if (it.type == Action.Type.NOTIFY) shouldNotify = true
                if (it.type == Action.Type.DONT_NOTIFY) shouldNotify = false
                if (it.type == Action.Type.SET_TWEAK) {
                    if (it.tweak_action == "highlight") highlight = it.boolValue ?: false
                    if (it.tweak_action == "sound") sound = it.stringValue
                }
            }
            return NotificationAction(shouldNotify, highlight, sound)
        }
    }
}