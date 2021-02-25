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

package im.vector.app.features.spaces.create

import com.airbnb.epoxy.TypedEpoxyController
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.GenericItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.form.formEditTextItem
import javax.inject.Inject

class SpaceDefaultRoomEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<CreateSpaceState>() {

    var listener: Listener? = null

    override fun buildModels(data: CreateSpaceState?) {
        genericFooterItem {
            id("info_help_header")
            style(GenericItem.STYLE.BIG_TEXT)
            text(stringProvider.getString(R.string.create_spaces_room_public_header))
            textColor(colorProvider.getColorFromAttribute(R.attr.riot_primary_text_color))
        }

        genericFooterItem {
            id("info_help")
            text(stringProvider.getString(R.string.create_spaces_room_public_header_desc))
            textColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary))
        }

        formEditTextItem {
            id("roomName1")
            enabled(true)
            value(data?.defaultRooms?.get(0))
            hint(stringProvider.getString(R.string.create_room_name_section))
            endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
            showBottomSeparator(false)
            onTextChange { text ->
                listener?.onNameChange(0, text)
            }
        }

        formEditTextItem {
            id("roomName2")
            enabled(true)
            value(data?.defaultRooms?.get(1))
            hint(stringProvider.getString(R.string.create_room_name_section))
            endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
            showBottomSeparator(false)
            onTextChange { text ->
                listener?.onNameChange(1, text)
            }
        }

        formEditTextItem {
            id("roomName3")
            enabled(true)
            value(data?.defaultRooms?.get(2))
            hint(stringProvider.getString(R.string.create_room_name_section))
            endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
            showBottomSeparator(false)
            onTextChange { text ->
                listener?.onNameChange(2, text)
            }
        }
    }

    interface Listener {
        //        fun onAvatarDelete()
//        fun onAvatarChange()
        fun onNameChange(index: Int, newName: String)
//        fun onTopicChange(newTopic: String)
    }
}
