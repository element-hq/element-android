/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.devtools

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class RoomDevToolRootController @Inject constructor(
        private val stringProvider: StringProvider
) : EpoxyController() {

    var interactionListener: DevToolsInteractionListener? = null

    init {
        requestModelBuild()
    }

    override fun buildModels() {
        val host = this
        genericButtonItem {
            id("explore")
            text(host.stringProvider.getString(CommonStrings.dev_tools_explore_room_state))
            buttonClickAction {
                host.interactionListener?.processAction(RoomDevToolAction.ExploreRoomState)
            }
        }
        genericButtonItem {
            id("send")
            text(host.stringProvider.getString(CommonStrings.dev_tools_send_custom_event))
            buttonClickAction {
                host.interactionListener?.processAction(RoomDevToolAction.SendCustomEvent(false))
            }
        }
        genericButtonItem {
            id("send_state")
            text(host.stringProvider.getString(CommonStrings.dev_tools_send_state_event))
            buttonClickAction {
                host.interactionListener?.processAction(RoomDevToolAction.SendCustomEvent(true))
            }
        }
    }
}
