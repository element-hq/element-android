/*
 * Copyright 2020 New Vector Ltd
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

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_state)
abstract class CallTileTimelineItem : AbsBaseMessageItem<CallTileTimelineItem.Holder>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    override fun isCacheable() = false

    @EpoxyAttribute
    lateinit var attributes: Attributes

    override fun getViewStubId() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.endGuideline.updateLayoutParams<RelativeLayout.LayoutParams> {
            this.marginEnd = leftGuideline
        }
        holder.creatorNameView.text = attributes.userOfInterest.getBestName()
        attributes.avatarRenderer.render(attributes.userOfInterest, holder.creatorAvatarView)
        when (attributes.callStatus) {
            CallStatus.INVITED  -> renderInvitedStatus(holder)
            CallStatus.IN_CALL  -> renderInCallStatus(holder)
            CallStatus.REJECTED -> renderRejectedStatus(holder)
            CallStatus.ENDED    -> renderEndedStatus(holder)
            CallStatus.MISSED   -> renderMissedStatus(holder)
        }
        renderSendState(holder.view, null, holder.failedToSendIndicator)
    }

    private fun renderMissedStatus(holder: Holder) {
        // Sent by me means I made the call and opponent missed it.
        if (attributes.informationData.sentByMe) {
            if (attributes.callKind.isVoiceCall) {
                holder.statusView.setStatus(R.string.call_tile_no_answer, R.drawable.ic_voice_call_declined)
            } else {
                holder.statusView.setStatus(R.string.call_tile_no_answer, R.drawable.ic_video_call_declined)
            }
        } else {
            if (attributes.callKind.isVoiceCall) {
                holder.statusView.setStatus(R.string.call_tile_voice_missed, R.drawable.ic_missed_voice_call_small)
            } else {
                holder.statusView.setStatus(R.string.call_tile_video_missed, R.drawable.ic_missed_video_call_small)
            }
        }
        holder.acceptRejectViewGroup.isVisible = true
        holder.acceptView.setText(R.string.call_tile_call_back)
        holder.acceptView.setLeftDrawable(attributes.callKind.icon, R.attr.colorOnPrimary)
        holder.acceptView.onClick {
            val callbackAction = RoomDetailAction.StartCall(attributes.callKind == CallKind.VIDEO)
            attributes.callback?.onTimelineItemAction(callbackAction)
        }
        holder.rejectView.isVisible = false
    }

    private fun renderEndedStatus(holder: Holder) {
        holder.acceptRejectViewGroup.isVisible = false
        when (attributes.callKind) {
            CallKind.VIDEO      -> {
                val endCallStatus = holder.resources.getString(R.string.call_tile_video_call_has_ended, attributes.formattedDuration)
                holder.statusView.setStatus(endCallStatus)
            }
            CallKind.AUDIO      -> {
                val endCallStatus = holder.resources.getString(R.string.call_tile_voice_call_has_ended, attributes.formattedDuration)
                holder.statusView.setStatus(endCallStatus)
            }
            CallKind.CONFERENCE -> {
                holder.statusView.setStatus(R.string.call_tile_ended)
            }
        }
    }

    private fun renderRejectedStatus(holder: Holder) {
        holder.acceptRejectViewGroup.isVisible = true
        holder.acceptView.setText(R.string.call_tile_call_back)
        holder.acceptView.setLeftDrawable(attributes.callKind.icon, R.attr.colorOnPrimary)
        holder.acceptView.onClick {
            val callbackAction = RoomDetailAction.StartCall(attributes.callKind == CallKind.VIDEO)
            attributes.callback?.onTimelineItemAction(callbackAction)
        }
        holder.rejectView.isVisible = false
        // Sent by me means I rejected the call made by opponent.
        if (attributes.informationData.sentByMe) {
            if (attributes.callKind.isVoiceCall) {
                holder.statusView.setStatus(R.string.call_tile_voice_declined, R.drawable.ic_voice_call_declined)
            } else {
                holder.statusView.setStatus(R.string.call_tile_video_declined, R.drawable.ic_video_call_declined)
            }
        } else {
            if (attributes.callKind.isVoiceCall) {
                holder.statusView.setStatus(R.string.call_tile_no_answer, R.drawable.ic_voice_call_declined)
            } else {
                holder.statusView.setStatus(R.string.call_tile_no_answer, R.drawable.ic_video_call_declined)
            }
        }
    }

    private fun renderInCallStatus(holder: Holder) {
        holder.acceptRejectViewGroup.isVisible = true
        holder.acceptView.isVisible = false
        when {
            attributes.callKind == CallKind.CONFERENCE -> {
                holder.rejectView.isVisible = true
                holder.rejectView.setText(R.string.action_leave)
                holder.rejectView.setLeftDrawable(R.drawable.ic_call_hangup, R.attr.colorOnPrimary)
                holder.rejectView.onClick {
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.LeaveJitsiCall)
                }
            }
            attributes.isStillActive                   -> {
                holder.rejectView.isVisible = true
                holder.rejectView.setText(R.string.call_notification_hangup)
                holder.rejectView.setLeftDrawable(R.drawable.ic_call_hangup, R.attr.colorOnPrimary)
                holder.rejectView.onClick {
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.EndCall)
                }
            }
            else                                       -> {
                holder.acceptRejectViewGroup.isVisible = false
            }
        }
        if (attributes.callKind.isVoiceCall) {
            holder.statusView.setStatus(R.string.call_tile_voice_active)
        } else {
            holder.statusView.setStatus(R.string.call_tile_video_active)
        }
    }

    private fun renderInvitedStatus(holder: Holder) {
        when {
            attributes.callKind == CallKind.CONFERENCE                       -> {
                holder.acceptRejectViewGroup.isVisible = true
                holder.acceptView.onClick {
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.JoinJitsiCall)
                }
                holder.acceptView.isVisible = true
                holder.rejectView.isVisible = false
                holder.acceptView.setText(R.string.action_join)
                holder.acceptView.setLeftDrawable(R.drawable.ic_call_video_small, R.attr.colorOnPrimary)
            }
            !attributes.informationData.sentByMe && attributes.isStillActive -> {
                holder.acceptRejectViewGroup.isVisible = true
                holder.acceptView.isVisible = true
                holder.rejectView.isVisible = true
                holder.acceptView.onClick {
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.AcceptCall(callId = attributes.callId))
                }
                holder.rejectView.setLeftDrawable(R.drawable.ic_call_hangup, R.attr.colorOnPrimary)
                holder.rejectView.onClick {
                    attributes.callback?.onTimelineItemAction(RoomDetailAction.EndCall)
                }
                if (attributes.callKind == CallKind.AUDIO) {
                    holder.rejectView.setText(R.string.call_notification_reject)
                    holder.acceptView.setText(R.string.call_notification_answer)
                    holder.acceptView.setLeftDrawable(R.drawable.ic_call_audio_small, R.attr.colorOnPrimary)
                } else if (attributes.callKind == CallKind.VIDEO) {
                    holder.rejectView.setText(R.string.call_notification_reject)
                    holder.acceptView.setText(R.string.call_notification_answer)
                    holder.acceptView.setLeftDrawable(R.drawable.ic_call_video_small, R.attr.colorOnPrimary)
                }
            }
            else                                                             -> {
                holder.acceptRejectViewGroup.isVisible = false
            }
        }
        when {
            // Invite state for conference should show as InCallStatus
            attributes.callKind == CallKind.CONFERENCE -> {
                holder.statusView.setStatus(R.string.call_tile_video_active)
            }
            attributes.informationData.sentByMe -> {
                holder.statusView.setStatus(R.string.call_ringing)
            }
            attributes.callKind.isVoiceCall     -> {
                holder.statusView.setStatus(R.string.call_tile_voice_incoming)
            }
            else                                -> {
                holder.statusView.setStatus(R.string.call_tile_video_incoming)
            }
        }
    }

    private fun TextView.setStatus(@StringRes statusRes: Int, @DrawableRes drawableRes: Int? = null) {
        val status = resources.getString(statusRes)
        setStatus(status, drawableRes)
    }

    private fun TextView.setStatus(status: String, @DrawableRes drawableRes: Int? = null) {
        setLeftDrawable(drawableRes ?: attributes.callKind.icon)
        text = status
    }

    class Holder : AbsBaseMessageItem.Holder(STUB_ID) {
        val acceptView by bind<Button>(R.id.itemCallAcceptView)
        val rejectView by bind<Button>(R.id.itemCallRejectView)
        val acceptRejectViewGroup by bind<ViewGroup>(R.id.itemCallAcceptRejectViewGroup)
        val creatorAvatarView by bind<ImageView>(R.id.itemCallCreatorAvatar)
        val creatorNameView by bind<TextView>(R.id.itemCallCreatorNameTextView)
        val statusView by bind<TextView>(R.id.itemCallStatusTextView)
        val endGuideline by bind<View>(R.id.messageEndGuideline)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)

        val resources: Resources
            get() = view.context.resources
    }

    companion object {
        private const val STUB_ID = R.id.messageCallStub
    }

    data class Attributes(
            val callId: String,
            val callKind: CallKind,
            val callStatus: CallStatus,
            val userOfInterest: MatrixItem,
            val isStillActive: Boolean,
            val formattedDuration: String,
            val callback: TimelineEventController.Callback? = null,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val messageColorProvider: MessageColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: ClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            override val reactionsSummaryEvents: ReactionsSummaryEvents? = null
    ) : AbsBaseMessageItem.Attributes

    enum class CallKind(@DrawableRes val icon: Int, @StringRes val title: Int) {
        VIDEO(R.drawable.ic_call_video_small, R.string.action_video_call),
        AUDIO(R.drawable.ic_call_audio_small, R.string.action_voice_call),
        CONFERENCE(R.drawable.ic_call_video_small, R.string.action_video_call);

        val isVoiceCall
            get() = this == AUDIO
    }

    enum class CallStatus {
        INVITED,
        IN_CALL,
        REJECTED,
        MISSED,
        ENDED;

        fun isActive() = this == INVITED || this == IN_CALL
    }
}
