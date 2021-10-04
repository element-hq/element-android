/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.item

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.button.MaterialButton
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.session.room.model.message.MessageOptionsContent

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageOptionsItem : AbsMessageItem<MessageOptionsItem.Holder>() {

    @EpoxyAttribute
    var optionsContent: MessageOptionsContent? = null

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var informationData: MessageInformationData? = null

    override fun getViewType() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)

        renderSendState(holder.view, holder.labelText)

        holder.labelText.setTextOrHide(optionsContent?.label)

        holder.buttonContainer.removeAllViews()

        val relatedEventId = informationData?.eventId ?: return
        val options = optionsContent?.options?.takeIf { it.isNotEmpty() } ?: return
        // Now add back the buttons
        options.forEachIndexed { index, option ->
            val materialButton = LayoutInflater.from(holder.view.context).inflate(R.layout.option_buttons, holder.buttonContainer, false)
                    as MaterialButton
            holder.buttonContainer.addView(materialButton, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            materialButton.text = option.label
            materialButton.onClick {
                callback?.onTimelineItemAction(RoomDetailAction.ReplyToOptions(relatedEventId, index, option.value ?: "$index"))
            }
        }
    }

    class Holder : AbsMessageItem.Holder(STUB_ID) {

        val labelText by bind<TextView>(R.id.optionLabelText)

        val buttonContainer by bind<ViewGroup>(R.id.optionsButtonContainer)
    }

    companion object {
        private const val STUB_ID = R.id.messageOptionsStub
    }
}
