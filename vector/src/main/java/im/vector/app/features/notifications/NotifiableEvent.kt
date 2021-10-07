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

import java.io.Serializable

/**
 * Parent interface for all events which can be displayed as a Notification
 */
sealed interface NotifiableEvent : Serializable {
    val eventId: String
    val editedEventId: String?

    // Used to know if event should be replaced with the one coming from eventstream
    val canBeReplaced: Boolean
    val isRedacted: Boolean
}
