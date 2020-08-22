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

package im.vector.app.features.roomprofile.members

import com.airbnb.epoxy.TypedEpoxyController
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomThirdPartyInviteContent
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import im.vector.app.R
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.epoxy.profiles.profileMatrixItem
import im.vector.app.core.extensions.join
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
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
        val threePidInvites = data.threePidInvites().orEmpty()
        var threePidInvitesDone = threePidInvites.isEmpty()

        for ((powerLevelCategory, roomMemberList) in roomMembersByPowerLevel) {
            if (roomMemberList.isEmpty()) {
                continue
            }

            if (powerLevelCategory == RoomMemberListCategories.USER && !threePidInvitesDone) {
                // If there is not regular invite, display threepid invite before the regular user
                buildProfileSection(
                        stringProvider.getString(RoomMemberListCategories.INVITE.titleRes)
                )

                buildThreePidInvites(data)
                threePidInvitesDone = true
            }

            buildProfileSection(
                    stringProvider.getString(powerLevelCategory.titleRes)
            )
            roomMemberList.join(
                    each = { _, roomMember ->
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
                    between = { _, roomMemberBefore ->
                        dividerItem {
                            id("divider_${roomMemberBefore.userId}")
                            color(dividerColor)
                        }
                    }
            )
            if (powerLevelCategory == RoomMemberListCategories.INVITE) {
                // Display the threepid invite after the regular invite
                dividerItem {
                    id("divider_threepidinvites")
                    color(dividerColor)
                }
                buildThreePidInvites(data)
                threePidInvitesDone = true
            }
        }

        if (!threePidInvitesDone) {
            // If there is not regular invite and no regular user, finally display threepid invite here
            buildProfileSection(
                    stringProvider.getString(RoomMemberListCategories.INVITE.titleRes)
            )

            buildThreePidInvites(data)
        }
    }

    private fun buildThreePidInvites(data: RoomMemberListViewState) {
        data.threePidInvites()
                ?.filter { it.content.toModel<RoomThirdPartyInviteContent>() != null }
                ?.join(
                        each = { idx, event ->
                            event.content.toModel<RoomThirdPartyInviteContent>()
                                    ?.let { content ->
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
                        },
                        between = { idx, _ ->
                            dividerItem {
                                id("divider3_$idx")
                                color(dividerColor)
                            }
                        }
                )
    }

    private fun RoomThirdPartyInviteContent.toMatrixItem(): MatrixItem {
        return MatrixItem.UserItem("@", displayName = displayName)
    }
}
