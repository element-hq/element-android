/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.item

import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class ElementCallTileTimelineItem : AbsBaseMessageItem<ElementCallTileTimelineItem.Holder>(R.layout.item_timeline_event_base_state) {

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
        renderSendState(holder.view, null, holder.failedToSendIndicator)
    }

    class Holder : AbsBaseMessageItem.Holder(STUB_ID) {
        val creatorAvatarView by bind<ImageView>(R.id.itemCallCreatorAvatar)
        val creatorNameView by bind<TextView>(R.id.itemCallCreatorNameTextView)
        val endGuideline by bind<View>(R.id.messageEndGuideline)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)

        val resources: Resources
            get() = view.context.resources
    }

    companion object {
        private val STUB_ID = R.id.messageElementCallStub
    }

    data class Attributes(
            val callId: String,
            val callKind: CallKind,
            val callStatus: CallStatus,
            val userOfInterest: MatrixItem,
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
        VIDEO(R.drawable.ic_call_video_small, CommonStrings.action_video_call),
    }

    enum class CallStatus {
        INVITED,
    }
}
