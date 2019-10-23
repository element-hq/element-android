/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.home.room.list.actions

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.action.bottomSheetItemAction
import im.vector.riotx.features.home.room.detail.timeline.action.bottomSheetItemSeparator
import javax.inject.Inject

/**
 * Epoxy controller for room list actions
 */
class RoomListQuickActionsEpoxyController @Inject constructor(private val stringProvider: StringProvider,
                                                              private val avatarRenderer: AvatarRenderer,
                                                              private val fontProvider: EmojiCompatFontProvider) : TypedEpoxyController<RoomListQuickActionsState>() {

    var listener: Listener? = null

    override fun buildModels(state: RoomListQuickActionsState) {

        // Separator
        bottomSheetItemSeparator {
            id("actions_separator")
        }

        // Actions
        state.quickActions()?.forEachIndexed { index, action ->
            bottomSheetItemAction {
                id("action_$index")
                iconRes(action.iconResId)
                textRes(action.titleRes)
                listener(View.OnClickListener { listener?.didSelectMenuAction(action) })
            }
        }
    }

    interface Listener {
        fun didSelectMenuAction(quickActions: RoomListQuickActions)
    }
}
