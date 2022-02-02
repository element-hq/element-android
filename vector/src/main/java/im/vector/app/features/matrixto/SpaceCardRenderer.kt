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

package im.vector.app.features.matrixto

import androidx.core.view.isGone
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentMatrixToRoomSpaceCardBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.app.features.home.room.detail.timeline.tools.linkify
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceCardRenderer @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider
) {

    fun render(spaceSummary: RoomSummary?,
               peopleYouKnow: List<User>,
               matrixLinkCallback: TimelineEventController.UrlClickCallback?,
               inCard: FragmentMatrixToRoomSpaceCardBinding,
               showDescription: Boolean) {
        if (spaceSummary == null) {
            inCard.matrixToCardContentVisibility.isVisible = false
            inCard.matrixToCardButtonLoading.isVisible = true
        } else {
            inCard.matrixToCardContentVisibility.isVisible = true
            inCard.matrixToCardButtonLoading.isVisible = false
            avatarRenderer.render(spaceSummary.toMatrixItem(), inCard.matrixToCardAvatar)
            inCard.matrixToCardNameText.text = spaceSummary.name
            inCard.matrixToCardAliasText.setTextOrHide(spaceSummary.canonicalAlias)
            inCard.matrixToCardDescText.setTextOrHide(spaceSummary.topic.linkify(matrixLinkCallback))
            if (spaceSummary.isPublic) {
                inCard.matrixToAccessText.setTextOrHide(stringProvider.getString(R.string.public_space))
                inCard.matrixToAccessImage.isVisible = true
                inCard.matrixToAccessImage.setImageResource(R.drawable.ic_public_room)
            } else {
                inCard.matrixToAccessText.setTextOrHide(stringProvider.getString(R.string.private_space))
                inCard.matrixToAccessImage.isVisible = true
                inCard.matrixToAccessImage.setImageResource(R.drawable.ic_room_private)
            }
            val memberCount = spaceSummary.joinedMembersCount ?: 0
            if (memberCount != 0) {
                inCard.matrixToMemberPills.isVisible = true
                inCard.spaceChildMemberCountText.text = stringProvider.getQuantityString(R.plurals.room_title_members, memberCount, memberCount)
            } else {
                // hide the pill
                inCard.matrixToMemberPills.isVisible = false
            }

            inCard.matrixToCardDescText.isVisible = showDescription

            renderPeopleYouKnow(inCard, peopleYouKnow.map { it.toMatrixItem() })
        }
        inCard.matrixToCardDescText.movementMethod = createLinkMovementMethod(object : TimelineEventController.UrlClickCallback {
            override fun onUrlClicked(url: String, title: String): Boolean {
                return false
            }

            override fun onUrlLongClicked(url: String): Boolean {
                // host.callback?.onUrlInTopicLongClicked(url)
                return true
            }
        })
    }

    fun render(spaceChildInfo: SpaceChildInfo?,
               peopleYouKnow: List<User>,
               matrixLinkCallback: TimelineEventController.UrlClickCallback?,
               inCard: FragmentMatrixToRoomSpaceCardBinding) {
        if (spaceChildInfo == null) {
            inCard.matrixToCardContentVisibility.isVisible = false
            inCard.matrixToCardButtonLoading.isVisible = true
        } else {
            inCard.matrixToCardContentVisibility.isVisible = true
            inCard.matrixToCardButtonLoading.isVisible = false
            avatarRenderer.render(spaceChildInfo.toMatrixItem(), inCard.matrixToCardAvatar)
            inCard.matrixToCardNameText.setTextOrHide(spaceChildInfo.name)
            inCard.matrixToCardAliasText.setTextOrHide(spaceChildInfo.canonicalAlias)
            inCard.matrixToCardDescText.setTextOrHide(spaceChildInfo.topic?.linkify(matrixLinkCallback))
            if (spaceChildInfo.worldReadable) {
                inCard.matrixToAccessText.setTextOrHide(stringProvider.getString(R.string.public_space))
                inCard.matrixToAccessImage.isVisible = true
                inCard.matrixToAccessImage.setImageResource(R.drawable.ic_public_room)
            } else {
                inCard.matrixToAccessText.setTextOrHide(stringProvider.getString(R.string.private_space))
                inCard.matrixToAccessImage.isVisible = true
                inCard.matrixToAccessImage.setImageResource(R.drawable.ic_room_private)
            }
            val memberCount = spaceChildInfo.activeMemberCount ?: 0
            if (memberCount != 0) {
                inCard.matrixToMemberPills.isVisible = true
                inCard.spaceChildMemberCountText.text = stringProvider.getQuantityString(R.plurals.room_title_members, memberCount, memberCount)
            } else {
                // hide the pill
                inCard.matrixToMemberPills.isVisible = false
            }

            renderPeopleYouKnow(inCard, peopleYouKnow.map { it.toMatrixItem() })
        }
    }

    fun renderPeopleYouKnow(inCard: FragmentMatrixToRoomSpaceCardBinding, peopleYouKnow: List<MatrixItem.UserItem>) {
        val images = listOf(
                inCard.knownMember1,
                inCard.knownMember2,
                inCard.knownMember3,
                inCard.knownMember4,
                inCard.knownMember5
        ).onEach { it.isGone = true }

        if (peopleYouKnow.isEmpty()) {
            inCard.peopleYouMayKnowText.isVisible = false
        } else {
            peopleYouKnow.forEachIndexed { index, item ->
                images[index].isVisible = true
                avatarRenderer.render(item, images[index])
            }
            inCard.peopleYouMayKnowText.setTextOrHide(
                    stringProvider.getQuantityString(R.plurals.space_people_you_know,
                            peopleYouKnow.count(),
                            peopleYouKnow.count()
                    )
            )
        }
    }
}
