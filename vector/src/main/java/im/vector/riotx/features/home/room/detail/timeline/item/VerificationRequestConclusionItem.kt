/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.home.room.detail.timeline.item

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_state)
abstract class VerificationRequestConclusionItem : AbsBaseMessageItem<VerificationRequestConclusionItem.Holder>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    @EpoxyAttribute
    lateinit var attributes: Attributes

    override fun getViewType() = STUB_ID

    @SuppressLint("SetTextI18n")
    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.endGuideline.updateLayoutParams<RelativeLayout.LayoutParams> {
            this.marginEnd = leftGuideline
        }
        holder.titleView.text = holder.view.context.getString(R.string.sas_verified)
        holder.descriptionView.text = "${attributes.informationData.memberName} (${attributes.informationData.senderId})"
    }

    class Holder : AbsBaseMessageItem.Holder(STUB_ID) {
        val titleView by bind<AppCompatTextView>(R.id.itemVerificationDoneTitleTextView)
        val descriptionView by bind<AppCompatTextView>(R.id.itemVerificationDoneDetailTextView)
        val endGuideline by bind<View>(R.id.messageEndGuideline)
    }

    companion object {
        private const val STUB_ID = R.id.messageVerificationDoneStub
    }

    /**
     * This class holds all the common attributes for timeline items.
     */
    data class Attributes(
            val toUserId: String,
            val toUserName: String,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val colorProvider: ColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: View.OnClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val emojiTypeFace: Typeface? = null
    ) : AbsBaseMessageItem.Attributes
}
