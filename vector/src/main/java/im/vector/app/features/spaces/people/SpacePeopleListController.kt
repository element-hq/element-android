/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.people

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.profiles.profileMatrixItemWithPowerLevel
import im.vector.app.core.extensions.join
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.members.RoomMemberListCategories
import im.vector.app.features.roomprofile.members.RoomMemberListViewState
import im.vector.app.features.roomprofile.members.RoomMemberSummaryFilter
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpacePeopleListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val colorProvider: ColorProvider,
        private val stringProvider: StringProvider,
        private val dimensionConverter: DimensionConverter,
        private val roomMemberSummaryFilter: RoomMemberSummaryFilter
) : TypedEpoxyController<RoomMemberListViewState>() {

    interface InteractionListener {
        fun onSpaceMemberClicked(roomMemberSummary: RoomMemberSummary)
        fun onInviteToSpaceSelected()
    }

    var listener: InteractionListener? = null

    override fun buildModels(data: RoomMemberListViewState?) {
        val host = this
        val memberSummaries = data?.roomMemberSummaries?.invoke()
        if (memberSummaries == null) {
            loadingItem { id("loading") }
            return
        }
        roomMemberSummaryFilter.filter = data.filter
        var foundCount = 0
        memberSummaries.forEach { memberEntry ->

            val filtered = memberEntry.second
                    .filter { roomMemberSummaryFilter.test(it) }
            if (filtered.isNotEmpty()) {
                dividerItem {
                    id("divider_type_${memberEntry.first.titleRes}")
                }
            }
            foundCount += filtered.size
            filtered
                    .join(
                            each = { _, roomMember ->
                                profileMatrixItemWithPowerLevel {
                                    id(roomMember.userId)
                                    matrixItem(roomMember.toMatrixItem())
                                    avatarRenderer(host.avatarRenderer)
                                    userVerificationLevel(data.trustLevelMap.invoke()?.get(roomMember.userId))
                                            .apply {
                                                val pl = host.toPowerLevelLabel(memberEntry.first)
                                                if (memberEntry.first == RoomMemberListCategories.INVITE) {
                                                    powerLevelLabel(
                                                            span {
                                                                span(host.stringProvider.getString(CommonStrings.invited)) {
                                                                    textColor = host.colorProvider.getColorFromAttribute(
                                                                            im.vector.lib.ui.styles.R.attr.vctr_content_secondary
                                                                    )
                                                                    textStyle = "bold"
                                                                    // fontFamily = "monospace"
                                                                }
                                                            }
                                                    )
                                                } else if (pl != null) {
                                                    powerLevelLabel(
                                                            span {
                                                                span(" $pl ") {
                                                                    backgroundColor = host.colorProvider.getColor(
                                                                            im.vector.lib.ui.styles.R.color.notification_accent_color
                                                                    )
                                                                    paddingTop = host.dimensionConverter.dpToPx(2)
                                                                    paddingBottom = host.dimensionConverter.dpToPx(2)
                                                                    textColor = host.colorProvider.getColorFromAttribute(
                                                                            com.google.android.material.R.attr.colorOnPrimary
                                                                    )
                                                                    textStyle = "bold"
                                                                    // fontFamily = "monospace"
                                                                }
                                                            }
                                                    )
                                                } else {
                                                    powerLevelLabel(null)
                                                }
                                            }

                                    clickListener {
                                        host.listener?.onSpaceMemberClicked(roomMember)
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

        if (foundCount == 0 && data.filter.isNotEmpty()) {
            // add the footer thing
            genericItem {
                id("not_found")
                title(
                        span {
                            +"\n"
                            +host.stringProvider.getString(CommonStrings.no_result_placeholder)
                        }.toEpoxyCharSequence()
                )
                description(
                        span {
                            +host.stringProvider.getString(CommonStrings.looking_for_someone_not_in_space, data.roomSummary.invoke()?.displayName ?: "")
                            +"\n"
                            span("Invite them") {
                                textColor = host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary)
                                textStyle = "bold"
                            }
                        }.toEpoxyCharSequence()
                )
                itemClickAction {
                    host.listener?.onInviteToSpaceSelected()
                }
            }
        }
    }

    private fun toPowerLevelLabel(categories: RoomMemberListCategories): String? {
        return when (categories) {
            RoomMemberListCategories.ADMIN -> stringProvider.getString(CommonStrings.power_level_admin)
            RoomMemberListCategories.MODERATOR -> stringProvider.getString(CommonStrings.power_level_moderator)
            else -> null
        }
    }
}
