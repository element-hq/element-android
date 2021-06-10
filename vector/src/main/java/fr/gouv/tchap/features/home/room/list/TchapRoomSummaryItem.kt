/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.home.room.list

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.amulyakhare.textdrawable.TextDrawable
import fr.gouv.tchap.core.ui.views.HexagonMaskView
import fr.gouv.tchap.core.utils.TchapRoomType
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.RoomSummaryFormatter
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_tchap_room)
abstract class TchapRoomSummaryItem : VectorEpoxyModel<TchapRoomSummaryItem.Holder>() {

    @EpoxyAttribute lateinit var typingMessage: CharSequence
    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute @JvmField var isDirect: Boolean = false
    @EpoxyAttribute @JvmField var isEncrypted: Boolean = false
    @EpoxyAttribute @JvmField var isPinned: Boolean = false
    @EpoxyAttribute lateinit var roomType: TchapRoomType

    // Used only for diff calculation
    @EpoxyAttribute lateinit var lastEvent: String

    // We use DoNotHash here as Spans are not implementing equals/hashcode
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var lastFormattedEvent: CharSequence
    @EpoxyAttribute lateinit var lastEventTime: CharSequence
    @EpoxyAttribute var encryptionTrustLevel: RoomEncryptionTrustLevel? = null
    @EpoxyAttribute var unreadNotificationCount: Int = 0
    @EpoxyAttribute var hasDisabledNotifications: Boolean = false
    @EpoxyAttribute var hasUnreadMessage: Boolean = false
    @EpoxyAttribute var hasExpectedAction: Boolean = false
    @EpoxyAttribute var hasDraft: Boolean = false
    @EpoxyAttribute var showHighlighted: Boolean = false
    @EpoxyAttribute var hasFailedSending: Boolean = false
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemLongClickListener: View.OnLongClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemClickListener: View.OnClickListener? = null
    @EpoxyAttribute var showSelected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener(itemClickListener)
        holder.rootView.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            itemLongClickListener?.onLongClick(it) ?: false
        }
        holder.titleView.text = TchapUtils.getNameFromDisplayName(matrixItem.getBestName())
        holder.lastEventTimeView.text = lastEventTime
        holder.lastEventView.text = lastFormattedEvent
        holder.unreadCounterBadgeView.text = RoomSummaryFormatter.formatUnreadMessagesCounter(unreadNotificationCount)
        holder.unreadIndentIndicator.isVisible = hasUnreadMessage
        holder.draftView.isVisible = hasDraft
        renderAvatar(holder)
        renderRoomType(holder)
        renderSelection(holder, showSelected)
        holder.typingView.setTextOrHide(typingMessage)
        holder.lastEventView.isInvisible = holder.typingView.isVisible
        holder.unreadCounterBadgeView.manageVisibility(unreadNotificationCount > 0, true)
        holder.disabledNotificationsBadge.manageVisibility(hasDisabledNotifications, true)
        holder.expectedActionBadgeView.manageVisibility(hasExpectedAction, false)
        holder.pinView.manageVisibility(isPinned, true)
    }

    override fun unbind(holder: Holder) {
        holder.rootView.setOnClickListener(null)
        holder.rootView.setOnLongClickListener(null)
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    private fun renderSelection(holder: Holder, isSelected: Boolean) {
        if (isSelected) {
            holder.avatarCheckedImageView.visibility = View.VISIBLE
            val backgroundColor = ContextCompat.getColor(holder.view.context, R.color.riotx_accent)
            val backgroundDrawable = TextDrawable.builder().buildRound("", backgroundColor)
            holder.avatarImageView.setImageDrawable(backgroundDrawable)
        } else {
            holder.avatarCheckedImageView.visibility = View.GONE
            avatarRenderer.render(matrixItem, holder.avatarImageView)
        }
    }

    private fun renderAvatar(holder: Holder) {
        holder.avatarImageView.visibility = if (isDirect) View.VISIBLE else View.GONE
        holder.avatarHexagonImageView.visibility = if (isDirect) View.GONE else View.VISIBLE

        avatarRenderer.render(
                matrixItem,
                if (isDirect)
                    holder.avatarImageView
                else
                    holder.avatarHexagonImageView.apply {
                        setBorderSettings(
                                ThemeUtils.getColor(holder.view.context, R.attr.avatar_border_color),
                                1
                        )
                    }
        )
    }

    private fun renderRoomType(holder: Holder) {
        var resource: Int? = null

        holder.domainNameView.apply {
            when (roomType) {
                TchapRoomType.DIRECT   -> {
                    text = TchapUtils.getDomainFromDisplayName(matrixItem.getBestName())
                    setTextColor(ThemeUtils.getColor(holder.view.context, R.attr.secondary_text_color))
                    resource = R.drawable.ic_tchap_room_lock_grey
                    visibility = View.VISIBLE
                }
                TchapRoomType.PRIVATE  -> {
                    text = holder.view.context.getString(R.string.tchap_room_private_room_type)
                    setTextColor(ContextCompat.getColor(holder.view.context, R.color.tchap_coral))
                    resource = R.drawable.ic_tchap_room_lock_red
                    visibility = View.VISIBLE
                }
                TchapRoomType.EXTERNAL -> {
                    text = holder.view.context.getString(R.string.tchap_room_extern_room_type)
                    setTextColor(ContextCompat.getColor(holder.view.context, R.color.tchap_pumpkin_orange))
                    resource = R.drawable.ic_tchap_room_lock_orange
                    visibility = View.VISIBLE
                }
                TchapRoomType.FORUM    -> {
                    text = holder.view.context.getString(R.string.tchap_room_forum_type)
                    setTextColor(ContextCompat.getColor(holder.view.context, R.color.tchap_jade_green))
                    resource = R.drawable.ic_tchap_forum
                    visibility = View.VISIBLE
                }
                else                   -> {
                    visibility = View.GONE
                }
            }
        }

        holder.avatarRoomTypeImageView.apply {
            resource?.let { setImageDrawable(ContextCompat.getDrawable(holder.view.context, it)) }
            manageVisibility(resource != null, false)
        }
    }

    private fun View.manageVisibility(shouldShow: Boolean, isViewGone: Boolean) {
        visibility = when {
            shouldShow -> View.VISIBLE
            isViewGone -> View.GONE
            else       -> View.INVISIBLE
        }
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.roomNameView)
        val unreadCounterBadgeView by bind<AppCompatTextView>(R.id.roomUnreadCounterBadgeView)
        val unreadIndentIndicator by bind<View>(R.id.roomUnreadIndicator)
        val lastEventView by bind<TextView>(R.id.roomLastEventView)
        val domainNameView by bind<TextView>(R.id.roomDomainNameView)
        val typingView by bind<TextView>(R.id.roomTypingView)
        val draftView by bind<ImageView>(R.id.roomDraftBadge)
        val lastEventTimeView by bind<TextView>(R.id.roomLastEventTimeView)
        val avatarCheckedImageView by bind<ImageView>(R.id.roomAvatarCheckedImageView)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)
        val avatarHexagonImageView by bind<HexagonMaskView>(R.id.roomAvatarHexagonImageView)
        val avatarRoomTypeImageView by bind<ImageView>(R.id.roomAvatarEncryptedImageView)
        val disabledNotificationsBadge by bind<ImageView>(R.id.roomDisabledNotificationsBadge)
        val expectedActionBadgeView by bind<AppCompatTextView>(R.id.roomExpectedActionBadgeView)
        val pinView by bind<ImageView>(R.id.roomPin)
        val rootView by bind<ViewGroup>(R.id.itemRoomLayout)
    }
}
