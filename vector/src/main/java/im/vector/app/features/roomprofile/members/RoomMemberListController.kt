/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.epoxy.profiles.profileMatrixItem
import im.vector.app.core.epoxy.profiles.profileMatrixItemWithPowerLevelWithPresence
import im.vector.app.core.extensions.join
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.permissions.RoleFormatter
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomThirdPartyInviteContent
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomMemberListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val roomMemberSummaryFilter: RoomMemberSummaryFilter,
        private val roleFormatter: RoleFormatter,
) : TypedEpoxyController<RoomMemberListViewState>() {

    interface Callback {
        fun onRoomMemberClicked(roomMember: RoomMemberSummary)
        fun onThreePidInviteClicked(event: Event)
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomMemberListViewState?) {
        data ?: return
        val host = this

        roomMemberSummaryFilter.filter = data.filter

        val roomMembersByPowerLevel = data.roomMemberSummaries.invoke() ?: return
        val filteredThreePidInvites = data.threePidInvites()
                ?.filter { event ->
                    event.content.toModel<RoomThirdPartyInviteContent>()
                            ?.takeIf {
                                data.filter.isEmpty() || it.displayName?.contains(data.filter, ignoreCase = true) == true
                            } != null
                }
                .orEmpty()
        var threePidInvitesDone = filteredThreePidInvites.isEmpty()

        for ((category, roomMemberList) in roomMembersByPowerLevel) {
            val filteredRoomMemberList = roomMemberList.filter { roomMemberSummaryFilter.test(it.summary) }
            if (filteredRoomMemberList.isEmpty()) {
                continue
            }

            if (category == RoomMemberListCategories.USER && !threePidInvitesDone) {
                // If there is no regular invite, display threepid invite before the regular user
                buildProfileSection(
                        stringProvider.getString(RoomMemberListCategories.INVITE.titleRes)
                )

                buildThreePidInvites(filteredThreePidInvites, data.actionsPermissions.canRevokeThreePidInvite)
                threePidInvitesDone = true
            }

            buildProfileSection(
                    stringProvider.getString(category.titleRes)
            )

            filteredRoomMemberList.join(
                    each = { _, roomMember ->
                        buildRoomMember(roomMember, host, data)
                    },
                    between = { _, roomMemberBefore ->
                        dividerItem {
                            id("divider_${roomMemberBefore.summary.userId}")
                        }
                    }
            )
            if (category == RoomMemberListCategories.INVITE && !threePidInvitesDone) {
                // Display the threepid invite after the regular invite
                dividerItem {
                    id("divider_threepidinvites")
                }

                buildThreePidInvites(filteredThreePidInvites, data.actionsPermissions.canRevokeThreePidInvite)
                threePidInvitesDone = true
            }
        }

        if (!threePidInvitesDone) {
            // If there is not regular invite and no regular user, finally display threepid invite here
            buildProfileSection(
                    stringProvider.getString(RoomMemberListCategories.INVITE.titleRes)
            )

            buildThreePidInvites(filteredThreePidInvites, data.actionsPermissions.canRevokeThreePidInvite)
        }
    }

    private fun buildRoomMember(
            roomMember: RoomMemberWithPowerLevel,
            host: RoomMemberListController,
            data: RoomMemberListViewState
    ) {
        val role = Role.getSuggestedRole(roomMember.powerLevel)
        val powerLabel = roleFormatter.format(role)

        profileMatrixItemWithPowerLevelWithPresence {
            id(roomMember.summary.userId)
            matrixItem(roomMember.summary.toMatrixItem())
            avatarRenderer(host.avatarRenderer)
            userVerificationLevel(data.trustLevelMap.invoke()?.get(roomMember.summary.userId))
            clickListener {
                host.callback?.onRoomMemberClicked(roomMember.summary)
            }
            showPresence(true)
            userPresence(roomMember.summary.userPresence)
            ignoredUser(roomMember.summary.userId in data.ignoredUserIds)
            powerLevelLabel(
                    span {
                        span(powerLabel) {
                            textColor = host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                        }
                    }
            )
        }
    }

    private fun buildThreePidInvites(filteredThreePidInvites: List<Event>, canRevokeThreePidInvite: Boolean) {
        val host = this
        filteredThreePidInvites
                .join(
                        each = { idx, event ->
                            event.content.toModel<RoomThirdPartyInviteContent>()
                                    ?.let { content ->
                                        profileMatrixItem {
                                            id("3pid_$idx")
                                            matrixItem(MatrixItem.UserItem("@", displayName = content.displayName))
                                            avatarRenderer(host.avatarRenderer)
                                            editable(canRevokeThreePidInvite)
                                            clickListener {
                                                host.callback?.onThreePidInviteClicked(event)
                                            }
                                        }
                                    }
                        },
                        between = { idx, _ ->
                            dividerItem {
                                id("divider3_$idx")
                            }
                        }
                )
    }
}
