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
 *
 */

package im.vector.riotx.features.roomprofile

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.epoxy.profiles.profileItemAction
import im.vector.riotx.core.epoxy.profiles.profileItemSection
import javax.inject.Inject

class RoomProfileController @Inject constructor()
    : TypedEpoxyController<RoomProfileViewState>() {

    override fun buildModels(data: RoomProfileViewState?) {
        if (data == null) {
            return
        }

        profileItemSection {
            id("section_security")
            title("Security")
        }

        profileItemAction {
            id("action_learn_more")
            title("Learn more")
            editable(true)
            subtitle("Messages in this room are not end-to-end encrypted.")
        }

        dividerItem{
            id("action_learn_more_divider")
        }

        profileItemSection {
            id("section_options")
            title("Options")
        }

        profileItemAction {
            iconRes(R.drawable.ic_person_outline_black)
            id("action_member_list")
            title("88 people")
            editable(true)
        }

        dividerItem{
            id("action_member_list_divider")
        }

        profileItemAction {
            iconRes(R.drawable.ic_attachment)
            id("action_files")
            title("12 files")
            editable(true)
        }

        dividerItem{
            id("action_files_divider")
        }

        profileItemAction {
            iconRes(R.drawable.ic_settings_x)
            id("action_settings")
            title("Room settings")
            editable(true)
        }

    }
}
