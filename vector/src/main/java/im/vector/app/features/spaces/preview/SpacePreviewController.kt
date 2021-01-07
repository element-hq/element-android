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

package im.vector.app.features.spaces.preview

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItemHeader
import im.vector.app.core.utils.TextUtils
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.peeking.PeekResult
import org.matrix.android.sdk.internal.session.space.peeking.ISpaceChild
import org.matrix.android.sdk.internal.session.space.peeking.SpaceChildPeekResult
import org.matrix.android.sdk.internal.session.space.peeking.SpacePeekResult
import org.matrix.android.sdk.internal.session.space.peeking.SpaceSubChildPeekResult
import javax.inject.Inject

class SpacePreviewController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider
) : TypedEpoxyController<SpacePreviewState>() {

    interface InteractionListener

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: SpacePreviewState?) {
        val result: SpacePeekResult = data?.peekResult?.invoke() ?: return

        when (result) {
            is SpacePeekResult.SpacePeekError -> {
                genericFooterItem {
                    id("failed")
                    // TODO
                    text("Failed to resolve")
                }
            }
            is SpacePeekResult.Success -> {
                // add summary info
                val memberCount = result.summary.roomPeekResult.numJoinedMembers ?: 0

                spaceTopSummaryItem {
                    id("info")
                    formattedMemberCount(stringProvider.getQuantityString(R.plurals.room_title_members, memberCount, memberCount))
                    topic(result.summary.roomPeekResult.topic ?: "")
                }

                genericItemHeader {
                    id("header_rooms")
                    text(stringProvider.getString(R.string.rooms))
                }

                buildChildren(result.summary.children, 0)
            }
        }
    }

    private fun buildChildren(children: List<ISpaceChild>, depth: Int) {
        children.forEach { child ->
            when (child) {
                is SpaceSubChildPeekResult -> {
                    when (val roomPeekResult = child.roomPeekResult) {
                        is PeekResult.Success -> {
                            subSpaceItem {
                                id(roomPeekResult.roomId)
                                roomId(roomPeekResult.roomId)
                                title(roomPeekResult.name)
                                depth(depth)
                                avatarUrl(roomPeekResult.avatarUrl)
                                avatarRenderer(avatarRenderer)
                            }
                            buildChildren(child.children, depth + 1)
                        }
                        else -> {
                            // ?? TODO
                        }
                    }
                }
                is SpaceChildPeekResult    -> {
                    // We have to check if the peek result was success
                    when (val roomPeekResult = child.roomPeekResult) {
                        is PeekResult.Success -> {
                            roomChildItem {
                                id(child.id)
                                depth(depth)
                                roomId(roomPeekResult.roomId)
                                title(roomPeekResult.name ?: "")
                                topic(roomPeekResult.topic ?: "")
                                avatarUrl(roomPeekResult.avatarUrl)
                                memberCount(TextUtils.formatCountToShortDecimal(roomPeekResult.numJoinedMembers ?: 0))
                                avatarRenderer(avatarRenderer)
                            }
                        }
                        else                  -> {
                            // What to do here?
                        }
                    }
                }
            }
        }
    }
}
