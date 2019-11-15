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
import im.vector.riotx.core.resources.StringProvider
import javax.inject.Inject

class RoomProfileController @Inject constructor(private val stringProvider: StringProvider)
    : TypedEpoxyController<RoomProfileViewState>() {

    var callback: Callback? = null

    interface Callback {
        fun onLearnMoreClicked()
        fun onMemberListClicked()
        fun onSettingsClicked()
    }

    override fun buildModels(data: RoomProfileViewState?) {
        if (data == null) {
            return
        }

        val roomSummary = data.roomSummary()

        profileItemSection {
            id("section_security")
            title("Security")
        }


        val learnMoreSubtitle = if (data.isEncrypted) {
            R.string.room_profile_encrypted_subtitle
        } else {
            R.string.room_profile_not_encrypted_subtitle
        }
        profileItemAction {
            id("action_learn_more")
            title("Learn more")
            editable(true)
            subtitle(stringProvider.getString(learnMoreSubtitle))
            listener { _ ->
                callback?.onLearnMoreClicked()
            }
        }

        dividerItem {
            id("action_learn_more_divider")
        }

        profileItemSection {
            id("section_options")
            title("Options")
        }

        val numberOfMembers = (roomSummary?.otherMemberIds?.size ?: 0) + 1
        profileItemAction {
            iconRes(R.drawable.ic_person_outline_black)
            id("action_member_list")
            title(stringProvider.getString(R.string.room_profile_member_list_title, numberOfMembers))
            editable(true)
            listener { _ ->
                callback?.onMemberListClicked()
            }
        }

        dividerItem {
            id("action_member_list_divider")
        }

        profileItemAction {
            iconRes(R.drawable.ic_room_actions_settings)
            id("action_settings")
            title("Room settings")
            editable(true)
            listener { _ ->
                callback?.onSettingsClicked()
            }
        }

    }
}
