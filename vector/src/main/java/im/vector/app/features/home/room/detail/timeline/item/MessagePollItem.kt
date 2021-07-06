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

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.session.room.model.message.MessageOptionsContent
import kotlin.math.roundToInt

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
        val resultLines = listOf(holder.result1, holder.result2, holder.result3, holder.result4, holder.result5)

        buttons.forEach { it.isVisible = false }
        resultLines.forEach { it.isVisible = false }

        val myVote = informationData?.pollResponseAggregatedSummary?.myVote
        val iHaveVoted = myVote != null
        val votes = informationData?.pollResponseAggregatedSummary?.votes
        val totalVotes = votes?.values
                ?.fold(0) { acc, count -> acc + count } ?: 0
        val percentMode = totalVotes > 100

        if (!iHaveVoted) {
            // Show buttons if i have not voted
            holder.resultWrapper.isVisible = false
            optionsContent?.options?.forEachIndexed { index, item ->
                if (index < buttons.size) {
                    buttons[index].let {
                        // current limitation, have to wait for event to be sent in order to reply
                        it.isEnabled = informationData?.sendState?.isSent() ?: false
                        it.text = item.label
                        it.isVisible = true
                    }
                }
            }
        } else {
            holder.resultWrapper.isVisible = true
            val maxCount = votes?.maxByOrNull { it.value }?.value ?: 0
            optionsContent?.options?.forEachIndexed { index, item ->
                if (index < resultLines.size) {
                    val optionCount = votes?.get(index) ?: 0
                    val count = if (percentMode) {
                        if (totalVotes > 0) {
                            (optionCount / totalVotes.toFloat() * 100).roundToInt().let { "$it%" }
                        } else {
                            ""
                        }
                    } else {
                        optionCount.toString()
                    }
                    resultLines[index].let {
                        it.label = item.label
                        it.isWinner = optionCount == maxCount
                        it.optionSelected = index == myVote
                        it.percent = count
                        it.isVisible = true
                    }
                }
            }
        }
        holder.infoText.text = holder.view.context.resources.getQuantityString(R.plurals.poll_info, totalVotes, totalVotes)
    }

    override fun unbind(holder: Holder) {
        holder.pollId = null
        holder.callback = null
        holder.optionValues = null
        super.unbind(holder)
    }

    class Holder : AbsMessageItem.Holder(STUB_ID) {

        var pollId: String? = null
        var optionValues: List<String?>? = null
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

        val resultWrapper by bind<ViewGroup>(R.id.pollResultsWrapper)

        override fun bindView(itemView: View) {
            super.bindView(itemView)
            val buttons = listOf(button1, button2, button3, button4, button5)
            val clickListener = object : ClickListener {
                override fun invoke(p1: View) {
                    val optionIndex = buttons.indexOf(p1)
                    if (optionIndex != -1 && pollId != null) {
                        val compatValue = if (optionIndex < optionValues?.size ?: 0) optionValues?.get(optionIndex) else null
                        callback?.onTimelineItemAction(RoomDetailAction.ReplyToOptions(pollId!!, optionIndex, compatValue ?: "$optionIndex"))
                    }
                }
            }
            buttons.forEach { it.onClick(clickListener) }
        }
    }

    companion object {
        private const val STUB_ID = R.id.messagePollStub
    }
}
