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

package im.vector.app.features.roomprofile.settings.linkaccess

import com.airbnb.epoxy.TypedEpoxyController
import fr.gouv.tchap.core.utils.RoomUtils
import fr.gouv.tchap.core.utils.TchapRoomType
import im.vector.app.R
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.epoxy.profiles.profileActionItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.settingsInfoItem
import im.vector.app.features.form.formSwitchItem
import javax.inject.Inject

class TchapRoomLinkAccessController @Inject constructor(
        private val stringProvider: StringProvider
) : TypedEpoxyController<TchapRoomLinkAccessState>() {

    interface InteractionListener {
        fun setLinkAccessEnabled(isEnabled: Boolean)
        fun openAliasDetail(alias: String)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: TchapRoomLinkAccessState?) {
        data ?: return
        val roomSummary = data.roomSummary() ?: return
        val host = this

        buildProfileSection(
                stringProvider.getString(R.string.tchap_room_settings_room_access_by_link_title)
        )

        formSwitchItem {
            id("LinkAccessActivation")
            title(host.stringProvider.getString(R.string.tchap_room_settings_enable_room_access_by_link))
            switchChecked(data.isLinkAccessEnabled)
            listener { host.interactionListener?.setLinkAccessEnabled(it) }
        }

        settingsInfoItem {
            id("LinkAccessInfo")
            val roomType = RoomUtils.getRoomType(roomSummary)
            helperTextResId(
                    when {
                        !data.isLinkAccessEnabled          -> R.string.tchap_room_settings_enable_room_access_by_link_info_off
                        roomType == TchapRoomType.EXTERNAL -> R.string.tchap_room_settings_enable_room_access_by_link_info_on_with_limitation
                        else                               -> R.string.tchap_room_settings_enable_room_access_by_link_info_on
                    }
            )
        }

        if (data.isLinkAccessEnabled && !data.canonicalAlias.isNullOrEmpty()) {
            profileActionItem {
                id("canonical")
                title(data.canonicalAlias)
                listener { host.interactionListener?.openAliasDetail(data.canonicalAlias) }
            }
        }
    }
}
