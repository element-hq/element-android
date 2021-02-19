/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.devtools

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.events.model.Event

sealed class RoomDevToolAction : VectorViewModelAction {
    object ExploreRoomState : RoomDevToolAction()
    object OnBackPressed : RoomDevToolAction()
    object MenuEdit : RoomDevToolAction()
    object MenuItemSend : RoomDevToolAction()
    data class ShowStateEvent(val event: Event) : RoomDevToolAction()
    data class ShowStateEventType(val stateEventType: String) : RoomDevToolAction()
    data class UpdateContentText(val contentJson: String) : RoomDevToolAction()
    data class SendCustomEvent(val isStateEvent: Boolean) : RoomDevToolAction()
    data class CustomEventTypeChange(val type: String) : RoomDevToolAction()
    data class CustomEventContentChange(val content: String) : RoomDevToolAction()
    data class CustomEventStateKeyChange(val stateKey: String) : RoomDevToolAction()
}
