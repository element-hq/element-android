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

package im.vector.app.features.home.room.detail.timeline.item

import android.text.SpannableString
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.utils.tappableMatchingText
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.tools.linkify
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_noinfo)
abstract class MergedRoomCreationItem : BasedMergedItem<MergedRoomCreationItem.Holder>() {

    @EpoxyAttribute
    override lateinit var attributes: Attributes

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var movementMethod: MovementMethod? = null

    override fun getViewType() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)

        bindCreationSummaryTile(holder)

        if (attributes.isCollapsed) {
            // Take the oldest data
            val data = distinctMergeData.lastOrNull()

            val createdFromCurrentUser = data?.userId == attributes.currentUserId
            val summary = if (createdFromCurrentUser) {
                if (data?.isDirectRoom == true) {
                    holder.expandView.resources.getString(R.string.direct_room_created_summary_item_by_you)
                } else {
                    holder.expandView.resources.getString(R.string.room_created_summary_item_by_you)
                }
            } else {
                if (data?.isDirectRoom == true) {
                    holder.expandView.resources.getString(R.string.direct_room_created_summary_item, data.memberName)
                } else {
                    holder.expandView.resources.getString(R.string.room_created_summary_item, data?.memberName ?: data?.userId ?: "")
                }
            }
            holder.summaryView.text = summary
            holder.summaryView.visibility = View.VISIBLE
            holder.avatarView.visibility = View.VISIBLE
            if (data != null) {
                holder.avatarView.visibility = View.VISIBLE
                attributes.avatarRenderer.render(data.toMatrixItem(), holder.avatarView)
            } else {
                holder.avatarView.visibility = View.GONE
            }

            bindEncryptionTile(holder, data)
        } else {
            holder.avatarView.visibility = View.INVISIBLE
            holder.summaryView.visibility = View.GONE
            holder.encryptionTile.isGone = true
        }
    }

    private fun bindEncryptionTile(holder: Holder, data: Data?) {
        if (attributes.hasEncryptionEvent) {
            holder.encryptionTile.isVisible = true
            holder.encryptionTile.updateLayoutParams<ConstraintLayout.LayoutParams> {
                this.marginEnd = leftGuideline
            }
            if (attributes.isEncryptionAlgorithmSecure) {
                holder.e2eTitleTextView.text = holder.expandView.resources.getString(R.string.encryption_enabled)
                holder.e2eTitleDescriptionView.text = if (data?.isDirectRoom == true) {
                    holder.expandView.resources.getString(R.string.direct_room_encryption_enabled_tile_description)
                } else {
                    holder.expandView.resources.getString(R.string.encryption_enabled_tile_description)
                }
                holder.e2eTitleDescriptionView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                holder.e2eTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(holder.view.context, R.drawable.ic_shield_black),
                        null, null, null
                )
            } else {
                holder.e2eTitleTextView.text = holder.expandView.resources.getString(R.string.encryption_not_enabled)
                holder.e2eTitleDescriptionView.text = holder.expandView.resources.getString(R.string.encryption_unknown_algorithm_tile_description)
                holder.e2eTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(holder.view.context, R.drawable.ic_shield_warning),
                        null, null, null
                )
            }
        } else {
            holder.encryptionTile.isVisible = false
        }
    }

    private fun bindCreationSummaryTile(holder: Holder) {
        val roomSummary = attributes.roomSummary
        val roomDisplayName = roomSummary?.displayName
        holder.roomNameText.setTextOrHide(roomDisplayName)
        val isDirect = roomSummary?.isDirect == true
        val membersCount = roomSummary?.otherMemberIds?.size ?: 0

        if (isDirect) {
            holder.roomDescriptionText.text = holder.view.resources.getString(R.string.this_is_the_beginning_of_dm, roomSummary?.displayName ?: "")
        } else if (roomDisplayName.isNullOrBlank() || roomSummary.name.isBlank()) {
            holder.roomDescriptionText.text = holder.view.resources.getString(R.string.this_is_the_beginning_of_room_no_name)
        } else {
            holder.roomDescriptionText.text = holder.view.resources.getString(R.string.this_is_the_beginning_of_room, roomDisplayName)
        }

        val topic = roomSummary?.topic
        if (topic.isNullOrBlank()) {
            // do not show hint for DMs or group DMs
            if (!isDirect) {
                val addTopicLink = holder.view.resources.getString(R.string.add_a_topic_link_text)
                val styledText = SpannableString(holder.view.resources.getString(R.string.room_created_summary_no_topic_creation_text, addTopicLink))
                holder.roomTopicText.setTextOrHide(styledText.tappableMatchingText(addTopicLink, object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        attributes.callback?.onTimelineItemAction(RoomDetailAction.QuickActionSetTopic)
                    }
                }))
            }
        } else {
            holder.roomTopicText.setTextOrHide(
                    span {
                        span(holder.view.resources.getString(R.string.topic_prefix)) {
                            textStyle = "bold"
                        }
                        +topic.linkify(attributes.callback)
                    }
            )
        }
        holder.roomTopicText.movementMethod = movementMethod

        val roomItem = roomSummary?.toMatrixItem()
        val shouldSetAvatar = attributes.canChangeAvatar
                && (roomSummary?.isDirect == false || (isDirect && membersCount >= 2))
                && roomItem?.avatarUrl.isNullOrBlank()

        holder.roomAvatarImageView.isVisible = roomItem != null
        if (roomItem != null) {
            attributes.avatarRenderer.render(roomItem, holder.roomAvatarImageView)
            holder.roomAvatarImageView.onClick { view ->
                if (shouldSetAvatar) {
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.QuickActionSetAvatar)
                } else {
                    // Note: this is no op if there is no avatar on the room
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.ShowRoomAvatarFullScreen(roomItem, view))
                }
            }
        }

        holder.setAvatarButton.isVisible = shouldSetAvatar
        if (shouldSetAvatar) {
            holder.setAvatarButton.onClick {
                attributes.callback?.onTimelineItemAction(RoomDetailAction.QuickActionSetAvatar)
            }
        }

        holder.addPeopleButton.isVisible = !isDirect
        if (!isDirect) {
            holder.addPeopleButton.onClick {
                attributes.callback?.onTimelineItemAction(RoomDetailAction.QuickActionInvitePeople)
            }
        }
    }

    class Holder : BasedMergedItem.Holder(STUB_ID) {
        val summaryView by bind<TextView>(R.id.itemNoticeTextView)
        val avatarView by bind<ImageView>(R.id.itemNoticeAvatarView)
        val encryptionTile by bind<ViewGroup>(R.id.creationEncryptionTile)

        val e2eTitleTextView by bind<TextView>(R.id.itemVerificationDoneTitleTextView)
        val e2eTitleDescriptionView by bind<TextView>(R.id.itemVerificationDoneDetailTextView)

        val roomNameText by bind<TextView>(R.id.roomNameTileText)
        val roomDescriptionText by bind<TextView>(R.id.roomNameDescriptionText)
        val roomTopicText by bind<TextView>(R.id.roomNameTopicText)
        val roomAvatarImageView by bind<ImageView>(R.id.creationTileRoomAvatarImageView)
        val addPeopleButton by bind<View>(R.id.creationTileAddPeopleButton)
        val setAvatarButton by bind<View>(R.id.creationTileSetAvatarButton)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMergedCreationStub
    }

    data class Attributes(
            override val isCollapsed: Boolean,
            override val mergeData: List<Data>,
            override val avatarRenderer: AvatarRenderer,
            override val onCollapsedStateChanged: (Boolean) -> Unit,
            val callback: TimelineEventController.Callback? = null,
            val currentUserId: String,
            val hasEncryptionEvent: Boolean,
            val isEncryptionAlgorithmSecure: Boolean,
            val roomSummary: RoomSummary?,
            val canChangeAvatar: Boolean = false,
            val canChangeName: Boolean = false,
            val canChangeTopic: Boolean = false
    ) : BasedMergedItem.Attributes
}
