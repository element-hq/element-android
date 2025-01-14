/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.preview

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericHeaderItem
import im.vector.app.core.utils.TextUtils
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class SpacePreviewController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider
) : TypedEpoxyController<SpacePreviewState>() {

    interface InteractionListener

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: SpacePreviewState?) {
        val host = this
        val memberCount = data?.spaceInfo?.invoke()?.memberCount ?: 0

        spaceTopSummaryItem {
            id("info")
            formattedMemberCount(host.stringProvider.getQuantityString(CommonPlurals.room_title_members, memberCount, memberCount))
            topic(data?.spaceInfo?.invoke()?.topic ?: data?.topic ?: "")
        }

        val result = data?.childInfoList?.invoke() ?: return
        if (result.isNotEmpty()) {
            genericHeaderItem {
                id("header_rooms")
                text(host.stringProvider.getString(CommonStrings.rooms))
            }

            buildChildren(result, 0)
        }
    }

    private fun buildChildren(children: List<ChildInfo>, depth: Int) {
        val host = this
        children.forEach { child ->

            if (child.isSubSpace == true) {
                subSpaceItem {
                    id(child.roomId)
                    roomId(child.roomId)
                    title(child.name ?: "")
                    depth(depth)
                    avatarUrl(child.avatarUrl)
                    avatarRenderer(host.avatarRenderer)
                }
                when (child.children) {
                    is Loading -> {
                        loadingItem { id("loading_children_${child.roomId}") }
                    }
                    is Success -> {
                        buildChildren(child.children.invoke(), depth + 1)
                    }
                    else -> {
                    }
                }
            } else {
                roomChildItem {
                    id(child.roomId)
                    depth(depth)
                    roomId(child.roomId)
                    title(child.name ?: "")
                    topic(child.topic ?: "")
                    avatarUrl(child.avatarUrl)
                    memberCount(TextUtils.formatCountToShortDecimal(child.memberCount ?: 0))
                    avatarRenderer(host.avatarRenderer)
                }
            }
//            when (child) {
//                is SpaceSubChildPeekResult -> {
//                    when (val roomPeekResult = child.roomPeekResult) {
//                        is PeekResult.Success -> {
//                            subSpaceItem {
//                                id(roomPeekResult.roomId)
//                                roomId(roomPeekResult.roomId)
//                                title(roomPeekResult.name)
//                                depth(depth)
//                                avatarUrl(roomPeekResult.avatarUrl)
//                                avatarRenderer(avatarRenderer)
//                            }
//                            buildChildren(child.children, depth + 1)
//                        }
//                        else                  -> {
//                            // ?? TODO
//                        }
//                    }
//                }
//                is SpaceChildPeekResult -> {
//                    // We have to check if the peek result was success
//                    when (val roomPeekResult = child.roomPeekResult) {
//                        is PeekResult.Success -> {
//                            roomChildItem {
//                                id(child.id)
//                                depth(depth)
//                                roomId(roomPeekResult.roomId)
//                                title(roomPeekResult.name ?: "")
//                                topic(roomPeekResult.topic ?: "")
//                                avatarUrl(roomPeekResult.avatarUrl)
//                                memberCount(TextUtils.formatCountToShortDecimal(roomPeekResult.numJoinedMembers ?: 0))
//                                avatarRenderer(avatarRenderer)
//                            }
//                        }
//                        else                  -> {
//                            // What to do here?
//                        }
//                    }
//                }
//            }
        }
    }
}
