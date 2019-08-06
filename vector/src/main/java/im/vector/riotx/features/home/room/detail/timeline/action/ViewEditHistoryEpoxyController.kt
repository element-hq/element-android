/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.home.room.detail.timeline.action

import android.content.Context
import android.text.Spannable
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.util.ContentUtils.extractUsefulTextFromReply
import im.vector.riotx.R
import im.vector.riotx.core.extensions.localDateTime
import im.vector.riotx.core.ui.list.genericFooterItem
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.core.ui.list.genericItemHeader
import im.vector.riotx.core.ui.list.genericLoaderItem
import im.vector.riotx.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotx.features.html.EventHtmlRenderer
import me.gujun.android.span.span
import name.fraser.neil.plaintext.diff_match_patch
import timber.log.Timber
import java.util.*

/**
 * Epoxy controller for reaction event list
 */
class ViewEditHistoryEpoxyController(private val context: Context,
                                     val timelineDateFormatter: TimelineDateFormatter,
                                     val eventHtmlRenderer: EventHtmlRenderer) : TypedEpoxyController<ViewEditHistoryViewState>() {

    override fun buildModels(state: ViewEditHistoryViewState) {
        when (state.editList) {
            is Incomplete -> {
                genericLoaderItem {
                    id("Spinner")
                }
            }
            is Fail       -> {
                genericFooterItem {
                    id("failure")
                    text(context.getString(R.string.unknown_error))
                }
            }
            is Success    -> {
                state.editList()?.let { renderEvents(it, state.isOriginalAReply) }
            }

        }
    }

    private fun renderEvents(sourceEvents: List<Event>, isOriginalReply: Boolean) {
        if (sourceEvents.isEmpty()) {
            genericItem {
                id("footer")
                title(context.getString(R.string.no_message_edits_found))
            }
        } else {
            var lastDate: Calendar? = null
            sourceEvents.forEachIndexed { index, timelineEvent ->

                val evDate = Calendar.getInstance().apply {
                    timeInMillis = timelineEvent.originServerTs
                            ?: System.currentTimeMillis()
                }
                if (lastDate?.get(Calendar.DAY_OF_YEAR) != evDate.get(Calendar.DAY_OF_YEAR)) {
                    //need to display header with day
                    val dateString = if (DateUtils.isToday(evDate.timeInMillis)) context.getString(R.string.today)
                    else timelineDateFormatter.formatMessageDay(timelineEvent.localDateTime())
                    genericItemHeader {
                        id(evDate.hashCode())
                        text(dateString)
                    }
                }
                lastDate = evDate
                val cContent = getCorrectContent(timelineEvent, isOriginalReply)
                val body = cContent.second?.let { eventHtmlRenderer.render(it) }
                        ?: cContent.first

                val nextEvent = if (index + 1 <= sourceEvents.lastIndex) sourceEvents[index + 1] else null

                var spannedDiff: Spannable? = null
                if (nextEvent != null && cContent.second == null /*No diff for html*/) {
                    //compares the body
                    val nContent = getCorrectContent(nextEvent, isOriginalReply)
                    val nextBody = nContent.second?.let { eventHtmlRenderer.render(it) }
                            ?: nContent.first
                    val dmp = diff_match_patch()
                    val diff = dmp.diff_main(nextBody.toString(), body.toString())
                    Timber.e("#### Diff: $diff")
                    dmp.diff_cleanupSemantic(diff)
                    Timber.e("#### Diff: $diff")
                    spannedDiff = span {
                        diff.map {
                            when (it.operation) {
                                diff_match_patch.Operation.DELETE -> {
                                    span {
                                        text = it.text
                                        textColor = ContextCompat.getColor(context, R.color.vector_error_color)
                                        textDecorationLine = "line-through"
                                    }
                                }
                                diff_match_patch.Operation.INSERT -> {
                                    span {
                                        text = it.text
                                        textColor = ContextCompat.getColor(context, R.color.vector_success_color)
                                    }
                                }
                                else                              -> {
                                    span {
                                        text = it.text
                                    }
                                }
                            }
                        }

                    }
                }
                genericItem {
                    id(timelineEvent.eventId)
                    title(timelineDateFormatter.formatMessageHour(timelineEvent.localDateTime()))
                    description(spannedDiff ?: body)
                }
            }
        }
    }

    private fun getCorrectContent(event: Event, isOriginalReply: Boolean): Pair<String, String?> {
        val clearContent = event.getClearContent().toModel<MessageTextContent>()
        val newContent = clearContent
                ?.newContent
                ?.toModel<MessageTextContent>()
        if (isOriginalReply) {
            return extractUsefulTextFromReply(newContent?.body ?: clearContent?.body ?: "") to null
        }
        return (newContent?.body ?: clearContent?.body ?: "") to (newContent?.formattedBody
                ?: clearContent?.formattedBody)
    }
}