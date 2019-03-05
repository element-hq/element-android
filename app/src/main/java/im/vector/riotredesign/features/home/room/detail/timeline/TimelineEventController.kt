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

package im.vector.riotredesign.features.home.room.detail.timeline

import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.VisibilityState
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineData
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.epoxy.LoadingItemModel_
import im.vector.riotredesign.core.epoxy.RiotEpoxyModel
import im.vector.riotredesign.core.extensions.localDateTime
import im.vector.riotredesign.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.detail.timeline.item.DaySeparatorItem_
import im.vector.riotredesign.features.home.room.detail.timeline.paging.PagedListEpoxyController

class TimelineEventController(private val dateFormatter: TimelineDateFormatter,
                              private val timelineItemFactory: TimelineItemFactory,
                              private val timelineMediaSizeProvider: TimelineMediaSizeProvider
) : PagedListEpoxyController<TimelineEvent>(
        EpoxyAsyncUtil.getAsyncBackgroundHandler(),
        EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    private var isLoadingForward: Boolean = false
    private var isLoadingBackward: Boolean = false
    private var hasReachedEnd: Boolean = true

    var callback: Callback? = null

    fun update(timelineData: TimelineData?) {
        timelineData?.let {
            isLoadingForward = it.isLoadingForward
            isLoadingBackward = it.isLoadingBackward
            hasReachedEnd = it.events.lastOrNull()?.root?.type == EventType.STATE_ROOM_CREATE
            submitList(it.events)
            requestModelBuild()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        timelineMediaSizeProvider.recyclerView = recyclerView
    }

    override fun buildItemModels(currentPosition: Int, items: List<TimelineEvent?>): List<EpoxyModel<*>> {
        if (items.isNullOrEmpty()) {
            return emptyList()
        }
        val epoxyModels = ArrayList<EpoxyModel<*>>()
        val event = items[currentPosition] ?: return emptyList()
        val nextEvent = if (currentPosition + 1 < items.size) items[currentPosition + 1] else null

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

        timelineItemFactory.create(event, nextEvent, callback).also {
            it.id(event.localId)
            it.setOnVisibilityStateChanged(TimelineEventVisibilityStateChangedListener(callback, event, currentPosition))
            epoxyModels.add(it)
        }
        if (addDaySeparator) {
            val formattedDay = dateFormatter.formatMessageDay(date)
            val daySeparatorItem = DaySeparatorItem_().formattedDay(formattedDay).id(formattedDay)
            epoxyModels.add(daySeparatorItem)
        }
        return epoxyModels
    }

    override fun addModels(models: List<EpoxyModel<*>>) {
        LoadingItemModel_()
                .id("forward_loading_item")
                .addIf(isLoadingForward, this)

        super.add(models)

        LoadingItemModel_()
                .id("backward_loading_item")
                .addIf(!hasReachedEnd, this)
    }


    interface Callback {
        fun onEventVisible(event: TimelineEvent, index: Int)
        fun onUrlClicked(url: String)
    }

}

private class TimelineEventVisibilityStateChangedListener(private val callback: TimelineEventController.Callback?,
                                                          private val event: TimelineEvent,
                                                          private val currentPosition: Int)
    : RiotEpoxyModel.OnVisibilityStateChangedListener {

    override fun onVisibilityStateChanged(visibilityState: Int) {
        if (visibilityState == VisibilityState.VISIBLE) {
            callback?.onEventVisible(event, currentPosition)
        }
    }


}