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

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formMultiLineEditTextItem
import javax.inject.Inject

class RoomDevToolSendFormController @Inject constructor() : TypedEpoxyController<RoomDevToolViewState>() {

    interface InteractionListener {
        fun processAction(action: RoomDevToolAction)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: RoomDevToolViewState?) {
        val sendMode =  (data?.displayMode as? RoomDevToolViewState.Mode.SendEventForm)
                ?: return

        genericFooterItem {
            id("topSpace")
            text("")
        }
        formEditTextItem {
            id("event_type")
            enabled(true)
            value(data.sendEventDraft?.type)
            hint("Type")
            showBottomSeparator(false)
            onTextChange { text ->
                interactionListener?.processAction(RoomDevToolAction.CustomEventTypeChange(text))
            }
        }

        if (sendMode.isState) {
            formEditTextItem {
                id("state_key")
                enabled(true)
                value(data.sendEventDraft?.stateKey)
                hint("State Key")
                showBottomSeparator(false)
                onTextChange { text ->
                    interactionListener?.processAction(RoomDevToolAction.CustomEventStateKeyChange(text))
                }
            }
        }

        formMultiLineEditTextItem {
            id("event_content")
            enabled(true)
            value(data.sendEventDraft?.content)
            hint("Event Content")
            showBottomSeparator(false)
            onTextChange { text ->
                interactionListener?.processAction(RoomDevToolAction.CustomEventContentChange(text))
            }
        }
    }
}
