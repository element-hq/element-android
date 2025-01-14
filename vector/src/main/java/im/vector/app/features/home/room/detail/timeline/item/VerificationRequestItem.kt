/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.item

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationState

@EpoxyModelClass
abstract class VerificationRequestItem : AbsBaseMessageItem<VerificationRequestItem.Holder>(R.layout.item_timeline_event_base_state) {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    @EpoxyAttribute
    lateinit var attributes: Attributes

    @EpoxyAttribute
    lateinit var clock: Clock

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    override fun getViewStubId() = STUB_ID

    @SuppressLint("SetTextI18n")
    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.endGuideline.updateLayoutParams<RelativeLayout.LayoutParams> {
            this.marginEnd = leftGuideline
        }

        holder.titleView.text = if (attributes.informationData.sentByMe) {
            holder.view.context.getString(CommonStrings.verification_sent)
        } else {
            holder.view.context.getString(CommonStrings.verification_request)
        }

        holder.descriptionView.text = if (!attributes.informationData.sentByMe) {
            "${attributes.informationData.memberName} (${attributes.informationData.senderId})"
        } else {
            "${attributes.otherUserName} (${attributes.otherUserId})"
        }

        when (attributes.informationData.referencesInfoData?.verificationStatus) {
            VerificationState.REQUEST,
            null -> {
                holder.buttonBar.isVisible = !attributes.informationData.sentByMe
                holder.statusTextView.text = null
                holder.statusTextView.isVisible = false
            }
            VerificationState.CANCELED_BY_OTHER -> {
                holder.buttonBar.isVisible = false
                holder.statusTextView.text = holder.view.context
                        .getString(CommonStrings.verification_request_other_cancelled, attributes.informationData.memberName)
                holder.statusTextView.isVisible = true
            }
            VerificationState.CANCELED_BY_ME -> {
                holder.buttonBar.isVisible = false
                holder.statusTextView.text = holder.view.context.getString(CommonStrings.verification_request_you_cancelled)
                holder.statusTextView.isVisible = true
            }
            VerificationState.WAITING -> {
                holder.buttonBar.isVisible = false
                holder.statusTextView.text = holder.view.context.getString(CommonStrings.verification_request_waiting)
                holder.statusTextView.isVisible = true
            }
            VerificationState.DONE -> {
                holder.buttonBar.isVisible = false
                holder.statusTextView.text = if (attributes.informationData.sentByMe) {
                    holder.view.context.getString(CommonStrings.verification_request_other_accepted, attributes.otherUserName)
                } else {
                    holder.view.context.getString(CommonStrings.verification_request_you_accepted)
                }
                holder.statusTextView.isVisible = true
            }
        }

        // Always hide buttons if request is too old
        if (!VerificationService.isValidRequest(attributes.informationData.ageLocalTS, clock.epochMillis())) {
            holder.buttonBar.isVisible = false
        }

        holder.acceptButton.onClick {
            callback?.onTimelineItemAction(RoomDetailAction.AcceptVerificationRequest(attributes.referenceId, attributes.otherUserId))
        }
        holder.declineButton.onClick {
            callback?.onTimelineItemAction(RoomDetailAction.DeclineVerificationRequest(attributes.referenceId, attributes.otherUserId))
        }

        renderSendState(holder.view, null, holder.failedToSendIndicator)
    }

    class Holder : AbsBaseMessageItem.Holder(STUB_ID) {
        val titleView by bind<AppCompatTextView>(R.id.itemVerificationTitleTextView)
        val descriptionView by bind<AppCompatTextView>(R.id.itemVerificationDetailTextView)
        val buttonBar by bind<ViewGroup>(R.id.itemVerificationButtonBar)
        val statusTextView by bind<TextView>(R.id.itemVerificationStatusText)
        val endGuideline by bind<View>(R.id.messageEndGuideline)
        val declineButton by bind<Button>(R.id.sas_verification_verified_decline_button)
        val acceptButton by bind<Button>(R.id.sas_verification_verified_accept_button)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)
    }

    companion object {
        private val STUB_ID = R.id.messageVerificationRequestStub
    }

    /**
     * This class holds all the common attributes for timeline items.
     */
    data class Attributes(
            val otherUserId: String,
            val otherUserName: String,
            val referenceId: String,
//            val avatarSize: Int,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val messageColorProvider: MessageColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: ClickListener? = null,
//            val memberClickListener: ClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
//            val avatarCallback: TimelineEventController.AvatarCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            override val reactionsSummaryEvents: ReactionsSummaryEvents? = null,
            val emojiTypeFace: Typeface? = null,
    ) : AbsBaseMessageItem.Attributes
}
