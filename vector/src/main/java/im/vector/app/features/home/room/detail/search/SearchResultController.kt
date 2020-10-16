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

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.ui.list.genericItemHeader
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.search.EventAndSender
import org.matrix.android.sdk.api.util.toMatrixItem
import java.util.Calendar
import javax.inject.Inject

class SearchResultController @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val dateFormatter: VectorDateFormatter
) : TypedEpoxyController<SearchViewState>() {

    var listener: Listener? = null

    private var idx = 0

    interface Listener {
        fun onItemClicked(event: Event)
        fun loadMore()
    }

    init {
        setData(null)
    }

    override fun buildModels(data: SearchViewState?) {
        data ?: return

        if (data.hasMoreResult) {
            loadingItem {
                // Always use a different id, because we can be notified several times of visibility state changed
                id("loadMore${idx++}")
                onVisibilityStateChanged { _, _, visibilityState ->
                    if (visibilityState == VisibilityState.VISIBLE) {
                        listener?.loadMore()
                    }
                }
            }
        }

        buildSearchResultItems(data.searchResult)
    }

    private fun buildSearchResultItems(events: List<EventAndSender>) {
        var lastDate: Calendar? = null

        events.forEach { eventAndSender ->
            val eventDate = Calendar.getInstance().apply {
                timeInMillis = eventAndSender.event.originServerTs ?: System.currentTimeMillis()
            }
            if (lastDate?.get(Calendar.DAY_OF_YEAR) != eventDate.get(Calendar.DAY_OF_YEAR)) {
                genericItemHeader {
                    id(eventDate.hashCode())
                    text(dateFormatter.format(eventDate.timeInMillis, DateFormatKind.EDIT_HISTORY_HEADER))
                }
            }
            lastDate = eventDate

            searchResultItem {
                id(eventAndSender.event.eventId)
                avatarRenderer(avatarRenderer)
                dateFormatter(dateFormatter)
                event(eventAndSender.event)
                sender(eventAndSender.sender
                        ?: eventAndSender.event.senderId?.let { session.getUser(it) }?.toMatrixItem())
                listener { listener?.onItemClicked(eventAndSender.event) }
            }
        }
    }
}
