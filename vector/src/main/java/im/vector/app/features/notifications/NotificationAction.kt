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
package im.vector.app.features.notifications

import org.matrix.android.sdk.api.pushrules.Action

data class NotificationAction(
        val shouldNotify: Boolean,
        val highlight: Boolean,
        val soundName: String?
)

fun List<Action>.toNotificationAction(): NotificationAction {
    var shouldNotify = false
    var highlight = false
    var sound: String? = null
    forEach { action ->
        when (action) {
            is Action.Notify      -> shouldNotify = true
            is Action.DoNotNotify -> shouldNotify = false
            is Action.Highlight   -> highlight = action.highlight
            is Action.Sound       -> sound = action.sound
        }
    }
    return NotificationAction(shouldNotify, highlight, sound)
}
