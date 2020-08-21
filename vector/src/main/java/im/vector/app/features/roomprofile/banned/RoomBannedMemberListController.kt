/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.banned

import com.airbnb.epoxy.TypedEpoxyController
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import im.vector.app.R
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.epoxy.profiles.profileMatrixItemWithProgress
import im.vector.app.core.extensions.join
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.home.AvatarRenderer
import javax.inject.Inject

class RoomBannedMemberListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomBannedMemberListViewState>() {

    interface Callback {
        fun onUnbanClicked(roomMember: RoomMemberSummary)
    }

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    init {
        setData(null)
    }

    override fun buildModels(data: RoomBannedMemberListViewState?) {
        val bannedList = data?.bannedMemberSummaries?.invoke() ?: return

        val quantityString = stringProvider.getQuantityString(R.plurals.room_settings_banned_users_count, bannedList.size, bannedList.size)

        if (bannedList.isEmpty()) {
            buildProfileSection(stringProvider.getString(R.string.room_settings_banned_users_title))

            genericFooterItem {
                id("footer")
                text(quantityString)
            }
        } else {
            buildProfileSection(quantityString)

            bannedList.join(
                    each = { _, roomMember ->
                        val actionInProgress = data.onGoingModerationAction.contains(roomMember.userId)
                        profileMatrixItemWithProgress {
                            id(roomMember.userId)
                            matrixItem(roomMember.toMatrixItem())
                            avatarRenderer(avatarRenderer)
                            apply {
                                if (actionInProgress) {
                                    inProgress(true)
                                    editable(false)
                                } else {
                                    inProgress(false)
                                    editable(true)
                                    clickListener { _ ->
                                        callback?.onUnbanClicked(roomMember)
                                    }
                                }
                            }
                        }
                    },
                    between = { _, roomMemberBefore ->
                        dividerItem {
                            id("divider_${roomMemberBefore.userId}")
                            color(dividerColor)
                        }
                    }
            )
        }
    }
}
