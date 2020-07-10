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

package im.vector.riotx.features.roomprofile.members

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.session.room.model.RoomThirdPartyInviteContent
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.epoxy.profiles.buildProfileSection
import im.vector.riotx.core.epoxy.profiles.profileMatrixItem
import im.vector.riotx.core.extensions.join
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

class RoomMemberListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomMemberListViewState>() {

    interface Callback {
        fun onRoomMemberClicked(roomMember: RoomMemberSummary)
        fun onThreePidInvites(event: Event)
    }

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    init {
        setData(null)
    }

    override fun buildModels(data: RoomMemberListViewState?) {
        val roomMembersByPowerLevel = data?.roomMemberSummaries?.invoke() ?: return
        for ((powerLevelCategory, roomMemberList) in roomMembersByPowerLevel) {
            if (roomMemberList.isEmpty()) {
                continue
            }
            buildProfileSection(
                    stringProvider.getString(powerLevelCategory.titleRes)
            )
            roomMemberList.join(
                    each = { roomMember ->
                        profileMatrixItem {
                            id(roomMember.userId)
                            matrixItem(roomMember.toMatrixItem())
                            avatarRenderer(avatarRenderer)
                            userEncryptionTrustLevel(data.trustLevelMap.invoke()?.get(roomMember.userId))
                            clickListener { _ ->
                                callback?.onRoomMemberClicked(roomMember)
                            }
                        }
                    },
                    between = { roomMemberBefore ->
                        dividerItem {
                            id("divider_${roomMemberBefore.userId}")
                            color(dividerColor)
                        }
                    }
            )
        }
        buildThreePidInvites(data)
    }

    private fun buildThreePidInvites(data: RoomMemberListViewState) {
        if (data.threePidInvites().isNullOrEmpty()) {
            return
        }

        buildProfileSection(
                stringProvider.getString(R.string.room_member_power_level_three_pid_invites)
        )

        data.threePidInvites()?.forEachIndexed { idx, event ->
            val content = event.content.toModel<RoomThirdPartyInviteContent>() ?: return@forEachIndexed

            profileMatrixItem {
                id("3pid_$idx")
                matrixItem(content.toMatrixItem())
                avatarRenderer(avatarRenderer)
                editable(data.actionsPermissions.canRevokeThreePidInvite)
                clickListener { _ ->
                    callback?.onThreePidInvites(event)
                }
            }

        }
    }

    private fun RoomThirdPartyInviteContent.toMatrixItem(): MatrixItem {
        return MatrixItem.UserItem("@", displayName = displayName)
    }
}
