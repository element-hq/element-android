/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.banned

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.epoxy.profiles.profileMatrixItemWithProgress
import im.vector.app.core.extensions.join
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.members.RoomMemberSummaryFilter
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomBannedMemberListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        private val roomMemberSummaryFilter: RoomMemberSummaryFilter
) : TypedEpoxyController<RoomBannedMemberListViewState>() {

    interface Callback {
        fun onUnbanClicked(roomMember: RoomMemberSummary)
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomBannedMemberListViewState?) {
        val bannedList = data?.bannedMemberSummaries?.invoke() ?: return
        val host = this

        val quantityString = stringProvider.getQuantityString(CommonPlurals.room_settings_banned_users_count, bannedList.size, bannedList.size)

        if (bannedList.isEmpty()) {
            buildProfileSection(stringProvider.getString(CommonStrings.room_settings_banned_users_title))

            genericFooterItem {
                id("footer")
                text(quantityString.toEpoxyCharSequence())
            }
        } else {
            buildProfileSection(quantityString)

            roomMemberSummaryFilter.filter = data.filter
            bannedList
                    .filter { roomMemberSummaryFilter.test(it) }
                    .join(
                            each = { _, roomMember ->
                                val actionInProgress = data.onGoingModerationAction.contains(roomMember.userId)
                                profileMatrixItemWithProgress {
                                    id(roomMember.userId)
                                    matrixItem(roomMember.toMatrixItem())
                                    avatarRenderer(host.avatarRenderer)
                                    apply {
                                        if (actionInProgress) {
                                            inProgress(true)
                                            editable(false)
                                        } else {
                                            inProgress(false)
                                            editable(true)
                                            clickListener {
                                                host.callback?.onUnbanClicked(roomMember)
                                            }
                                        }
                                    }
                                }
                            },
                            between = { _, roomMemberBefore ->
                                dividerItem {
                                    id("divider_${roomMemberBefore.userId}")
                                }
                            }
                    )
        }
    }
}
