/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
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
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import org.matrix.android.sdk.api.util.toMatrixItem

@EpoxyModelClass
abstract class MergedRoomCreationItem : BasedMergedItem<MergedRoomCreationItem.Holder>(R.layout.item_timeline_event_base_noinfo) {

    @EpoxyAttribute
    override lateinit var attributes: Attributes

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var movementMethod: MovementMethod? = null

    private val roomSummary
        get() = attributes.roomSummary

    private val isDirectRoom
        get() = distinctMergeData.lastOrNull()?.isDirectRoom
                ?: roomSummary?.isDirect
                ?: false

    override fun getViewStubId() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)

        bindCreationSummaryTile(holder)
        bindMergedViews(holder)
    }

    private fun bindMergedViews(holder: Holder) {
        holder.mergedView.isVisible = !attributes.isLocalRoom
        if (attributes.isCollapsed) {
            // Take the oldest data
            val data = distinctMergeData.lastOrNull()
            renderSummaryText(holder, data)
            holder.summaryView.visibility = View.VISIBLE
            if (data != null) {
                holder.avatarView.visibility = View.VISIBLE
                attributes.avatarRenderer.render(data.toMatrixItem(), holder.avatarView)
            } else {
                holder.avatarView.visibility = View.GONE
            }
            bindEncryptionTile(holder)
        } else {
            holder.avatarView.visibility = View.INVISIBLE
            holder.summaryView.visibility = View.GONE
            holder.encryptionTile.visibility = View.GONE
        }
    }

    private fun renderSummaryText(holder: Holder, data: Data?) {
        val resources = holder.expandView.resources
        val createdFromCurrentUser = data?.userId == attributes.currentUserId
        val summary = if (createdFromCurrentUser) {
            if (isDirectRoom) {
                resources.getString(CommonStrings.direct_room_created_summary_item_by_you)
            } else {
                resources.getString(CommonStrings.room_created_summary_item_by_you)
            }
        } else {
            if (isDirectRoom) {
                resources.getString(CommonStrings.direct_room_created_summary_item, data?.memberName.orEmpty())
            } else {
                resources.getString(CommonStrings.room_created_summary_item, data?.memberName.orEmpty())
            }
        }
        holder.summaryView.text = summary
    }

    private fun bindEncryptionTile(holder: Holder) {
        if (attributes.hasEncryptionEvent) {
            holder.encryptionTile.isVisible = true
            holder.encryptionTile.updateLayoutParams<ConstraintLayout.LayoutParams> {
                this.marginEnd = leftGuideline
            }
            if (attributes.isEncryptionAlgorithmSecure) {
                renderE2ESecureTile(holder)
            } else {
                renderE2EUnsecureTile(holder)
            }
        } else {
            holder.encryptionTile.isVisible = false
        }
    }

    private fun renderE2ESecureTile(holder: Holder) {
        val (title, description, drawable) = when {
            isDirectRoom -> {
                val isWaitingUser = roomSummary?.isEncrypted.orFalse() && roomSummary?.joinedMembersCount == 1 && roomSummary?.invitedMembersCount == 0
                when {
                    attributes.isLocalRoom -> Triple(
                            CommonStrings.encryption_enabled,
                            CommonStrings.direct_room_encryption_enabled_tile_description_future,
                            R.drawable.ic_shield_black
                    )
                    isWaitingUser -> Triple(
                            CommonStrings.direct_room_encryption_enabled_waiting_users,
                            CommonStrings.direct_room_encryption_enabled_waiting_users_tile_description,
                            R.drawable.ic_room_profile_member_list
                    )
                    else -> Triple(
                            CommonStrings.encryption_enabled,
                            CommonStrings.direct_room_encryption_enabled_tile_description,
                            R.drawable.ic_shield_black
                    )
                }
            }
            else -> {
                Triple(CommonStrings.encryption_enabled, CommonStrings.encryption_enabled_tile_description, R.drawable.ic_shield_black)
            }
        }

        holder.e2eTitleTextView.text = holder.expandView.resources.getString(title)
        holder.e2eTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(holder.view.context, drawable),
                null, null, null
        )
        holder.e2eTitleDescriptionView.text = holder.expandView.resources.getString(description)
        holder.e2eTitleDescriptionView.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    private fun renderE2EUnsecureTile(holder: Holder) {
        holder.e2eTitleTextView.text = holder.expandView.resources.getString(CommonStrings.encryption_not_enabled)
        holder.e2eTitleDescriptionView.text = holder.expandView.resources.getString(CommonStrings.encryption_unknown_algorithm_tile_description)
        holder.e2eTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(holder.view.context, R.drawable.ic_shield_warning),
                null, null, null
        )
    }

    private fun bindCreationSummaryTile(holder: Holder) {
        val roomDisplayName = roomSummary?.displayName
        val membersCount = roomSummary?.otherMemberIds?.size ?: 0

        holder.roomNameText.setTextOrHide(roomDisplayName)
        renderRoomDescription(holder)
        renderRoomTopic(holder)

        val roomItem = roomSummary?.toMatrixItem()
        val shouldSetAvatar = attributes.canChangeAvatar &&
                (roomSummary?.isDirect == false || (isDirectRoom && membersCount >= 2)) &&
                roomItem?.avatarUrl.isNullOrBlank()

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

        val canInvite = attributes.canInvite && !isDirectRoom
        holder.addPeopleButton.isVisible = canInvite
        if (canInvite) {
            holder.addPeopleButton.onClick {
                attributes.callback?.onTimelineItemAction(RoomDetailAction.QuickActionInvitePeople)
            }
        }
    }

    private fun renderRoomDescription(holder: Holder) {
        val roomDisplayName = roomSummary?.displayName
        val resources = holder.roomDescriptionText.resources
        val description = when {
            isDirectRoom -> {
                if (attributes.isLocalRoom) {
                    resources.getString(CommonStrings.send_your_first_msg_to_invite, roomSummary?.displayName.orEmpty())
                } else {
                    resources.getString(CommonStrings.this_is_the_beginning_of_dm, roomSummary?.displayName.orEmpty())
                }
            }
            roomDisplayName.isNullOrBlank() || roomSummary?.name.isNullOrBlank() -> {
                holder.view.resources.getString(CommonStrings.this_is_the_beginning_of_room_no_name)
            }
            else -> {
                holder.view.resources.getString(CommonStrings.this_is_the_beginning_of_room, roomDisplayName)
            }
        }
        holder.roomDescriptionText.text = description
        if (isDirectRoom && attributes.isLocalRoom) {
            TextViewCompat.setTextAppearance(holder.roomDescriptionText, im.vector.lib.ui.styles.R.style.TextAppearance_Vector_Subtitle)
            holder.roomDescriptionText.setTextColor(
                    ThemeUtils.getColor(holder.roomDescriptionText.context, im.vector.lib.ui.styles.R.attr.vctr_content_primary)
            )
        }
    }

    private fun renderRoomTopic(holder: Holder) {
        val topic = roomSummary?.topic
        if (topic.isNullOrBlank()) {
            // do not show hint for DMs or group DMs
            val canSetTopic = attributes.canChangeTopic && !isDirectRoom
            if (canSetTopic) {
                val addTopicLink = holder.view.resources.getString(CommonStrings.add_a_topic_link_text)
                val styledText = SpannableString(holder.view.resources.getString(CommonStrings.room_created_summary_no_topic_creation_text, addTopicLink))
                holder.roomTopicText.setTextOrHide(styledText.tappableMatchingText(addTopicLink, object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        attributes.callback?.onTimelineItemAction(RoomDetailAction.QuickActionSetTopic)
                    }
                }))
            }
        } else {
            holder.roomTopicText.setTextOrHide(
                    span {
                        span(holder.view.resources.getString(CommonStrings.topic_prefix)) {
                            textStyle = "bold"
                        }
                        +topic.linkify(attributes.callback)
                    }
            )
        }
        holder.roomTopicText.movementMethod = movementMethod
    }

    class Holder : BasedMergedItem.Holder(STUB_ID) {
        val mergedView by bind<View>(R.id.mergedSumContainer)
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
        private val STUB_ID = R.id.messageContentMergedCreationStub
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
            val canInvite: Boolean = false,
            val canChangeAvatar: Boolean = false,
            val canChangeName: Boolean = false,
            val canChangeTopic: Boolean = false
    ) : BasedMergedItem.Attributes {

        val isLocalRoom = RoomLocalEcho.isLocalEchoId(roomSummary?.roomId.orEmpty())
    }
}
