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

package im.vector.app.features.home.room.detail.search

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.core.time.Clock
import im.vector.app.core.ui.list.GenericHeaderItem_
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.toMatrixItem
import java.util.Calendar
import javax.inject.Inject

class SearchResultController @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter,
        private val displayableEventFormatter: DisplayableEventFormatter,
        private val userPreferencesProvider: UserPreferencesProvider,
        private val clock: Clock,
) : TypedEpoxyController<SearchViewState>() {

    var listener: Listener? = null

    private var idx = 0

    interface Listener {
        fun onItemClicked(event: Event)
        fun onThreadSummaryClicked(event: Event)
        fun loadMore()
    }

    override fun buildModels(data: SearchViewState?) {
        data ?: return

        val host = this
        val searchItems = buildSearchResultItems(data)

        if (data.hasMoreResult) {
            loadingItem {
                // Always use a different id, because we can be notified several times of visibility state changed
                id("loadMore${host.idx++}")
                onVisibilityStateChanged { _, _, visibilityState ->
                    if (visibilityState == VisibilityState.VISIBLE) {
                        host.listener?.loadMore()
                    }
                }
            }
        } else {
            if (searchItems.isEmpty()) {
                // All returned results by the server has been filtered out and there is no more result
                noResultItem {
                    id("noResult")
                    text(host.stringProvider.getString(R.string.no_result_placeholder))
                }
            } else {
                noResultItem {
                    id("noMoreResult")
                    text(host.stringProvider.getString(R.string.no_more_results))
                }
            }
        }

        searchItems.forEach { add(it) }
    }

    /**
     * @return the list of EpoxyModel (date items and search result items), or an empty list if all items have been filtered out
     */
    private fun buildSearchResultItems(data: SearchViewState): List<EpoxyModel<*>> {
        var lastDate: Calendar? = null
        val result = mutableListOf<EpoxyModel<*>>()

        data.searchResult.forEach { eventAndSender ->
            val event = eventAndSender.event

            // Take new content first
            @Suppress("UNCHECKED_CAST")
            val text = ((event.content?.get("m.new_content") as? Content) ?: event.content)?.get("body") as? String ?: return@forEach
            val spannable = setHighLightedText(text, data.highlights) ?: return@forEach

            val eventDate = Calendar.getInstance().apply {
                timeInMillis = eventAndSender.event.originServerTs ?: clock.epochMillis()
            }
            if (lastDate?.get(Calendar.DAY_OF_YEAR) != eventDate.get(Calendar.DAY_OF_YEAR)) {
                GenericHeaderItem_()
                        .id(eventDate.hashCode())
                        .text(dateFormatter.format(eventDate.timeInMillis, DateFormatKind.EDIT_HISTORY_HEADER))
                        .let { result.add(it) }
            }
            lastDate = eventDate

            SearchResultItem_()
                    .id(eventAndSender.event.eventId)
                    .avatarRenderer(avatarRenderer)
                    .formattedDate(dateFormatter.format(event.originServerTs, DateFormatKind.MESSAGE_SIMPLE))
                    .spannable(spannable.toEpoxyCharSequence())
                    .sender(
                            eventAndSender.sender
                                    ?: eventAndSender.event.senderId?.let { session.roomService().getRoomMember(it, data.roomId) }?.toMatrixItem()
                    )
                    .threadDetails(event.threadDetails)
                    .threadSummaryFormatted(displayableEventFormatter.formatThreadSummary(event.threadDetails?.threadSummaryLatestEvent).toString())
                    .areThreadMessagesEnabled(userPreferencesProvider.areThreadMessagesEnabled())
                    .listener { listener?.onItemClicked(eventAndSender.event) }
                    .threadSummaryListener { listener?.onThreadSummaryClicked(eventAndSender.event) }
                    .let { result.add(it) }
        }

        return result
    }

    /**
     * Highlight the text. If the text is not found, return null to ignore this result
     * See https://github.com/matrix-org/synapse/issues/8686
     */
    private fun setHighLightedText(text: String, highlights: List<String>): Spannable? {
        val wordToSpan: Spannable = SpannableString(text)
        var found = false
        highlights.forEach { highlight ->
            var searchFromIndex = 0
            while (searchFromIndex < text.length) {
                val indexOfHighlight = text.indexOf(highlight, searchFromIndex, ignoreCase = true)
                searchFromIndex = if (indexOfHighlight == -1) {
                    Integer.MAX_VALUE
                } else {
                    // bold
                    found = true
                    wordToSpan.setSpan(StyleSpan(Typeface.BOLD), indexOfHighlight, indexOfHighlight + highlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    indexOfHighlight + 1
                }
            }
        }
        return wordToSpan.takeIf { found }
    }
}
