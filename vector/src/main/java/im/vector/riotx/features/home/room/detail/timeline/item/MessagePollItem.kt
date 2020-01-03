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

package im.vector.riotx.features.home.room.detail.timeline.item

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.api.session.room.model.message.MessageOptionsContent
import im.vector.riotx.R
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.features.home.room.detail.RoomDetailAction
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessagePollItem : AbsMessageItem<MessagePollItem.Holder>() {

    @EpoxyAttribute
    var optionsContent: MessageOptionsContent? = null

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var informationData: MessageInformationData? = null

    override fun getViewType() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.pollId = informationData?.eventId
        holder.callback = callback
        holder.optionValues = optionsContent?.options?.map { it.value ?: it.label }

        renderSendState(holder.view, holder.labelText)

        holder.labelText.setTextOrHide(optionsContent?.label)

        val buttons = listOf(holder.button1, holder.button2, holder.button3, holder.button4, holder.button5)

        buttons.forEach { it.isVisible = false }

        optionsContent?.options?.forEachIndexed { index, item ->
            if (index < buttons.size) {
                buttons[index].let {
                    it.text = item.label
                    it.isVisible = true
                }
            }
        }

        val resultLines = listOf(holder.result1, holder.result2, holder.result3, holder.result4, holder.result5)

        resultLines.forEach { it.isVisible = false }
        optionsContent?.options?.forEachIndexed { index, item ->
            if (index < resultLines.size) {
                resultLines[index].let {
                    it.label = item.label
                    it.optionSelected = index == 0
                    it.percent = "20%"
                    it.isVisible = true
                }
            }
        }
        holder.infoText.text = holder.view.context.resources.getQuantityString(R.plurals.poll_info, 0, 0)
    }

    override fun unbind(holder: Holder) {
        holder.pollId = null
        holder.callback = null
        holder.optionValues = null
        super.unbind(holder)
    }

    class Holder : AbsMessageItem.Holder(STUB_ID) {

        var pollId: String? = null
        var optionValues : List<String?>? = null
        var callback: TimelineEventController.Callback? = null

        val button1 by bind<Button>(R.id.pollButton1)
        val button2 by bind<Button>(R.id.pollButton2)
        val button3 by bind<Button>(R.id.pollButton3)
        val button4 by bind<Button>(R.id.pollButton4)
        val button5 by bind<Button>(R.id.pollButton5)

        val result1 by bind<PollResultLineView>(R.id.pollResult1)
        val result2 by bind<PollResultLineView>(R.id.pollResult2)
        val result3 by bind<PollResultLineView>(R.id.pollResult3)
        val result4 by bind<PollResultLineView>(R.id.pollResult4)
        val result5 by bind<PollResultLineView>(R.id.pollResult5)

        val labelText by bind<TextView>(R.id.pollLabelText)
        val infoText by bind<TextView>(R.id.pollInfosText)

        override fun bindView(itemView: View) {
            super.bindView(itemView)
            val buttons = listOf(button1, button2, button3, button4, button5)
            val clickListener = DebouncedClickListener(View.OnClickListener {
                val optionIndex = buttons.indexOf(it)
                if (optionIndex != -1 && pollId != null) {
                    val compatValue = if (optionIndex < optionValues?.size ?: 0) optionValues?.get(optionIndex) else null
                    callback?.onAction(RoomDetailAction.ReplyToPoll(pollId!!, optionIndex, compatValue ?: "$optionIndex"))
                }
            })
            buttons.forEach { it.setOnClickListener(clickListener) }
        }
    }

    companion object {
        private const val STUB_ID = R.id.messagePollStub
    }
}
