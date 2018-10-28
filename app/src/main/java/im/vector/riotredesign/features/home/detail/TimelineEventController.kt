package im.vector.riotredesign.features.home.detail

import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.riotredesign.features.home.LoadingItemModel_

class TimelineEventController : PagedListEpoxyController<Event>(
        diffingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    override fun buildItemModel(currentPosition: Int, item: Event?): EpoxyModel<*> {
        return if (item == null) {
            LoadingItemModel_().id(-currentPosition)
        } else {
            TimelineEventItem(item.toString()).id(item.eventId)
        }
    }

    init {
        isDebugLoggingEnabled = true
    }

    override fun onExceptionSwallowed(exception: RuntimeException) {
        throw exception
    }

}