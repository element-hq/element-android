package im.vector.riotredesign.features.home

import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.matrix.android.api.session.events.model.Event

class TimelineEventController : PagedListEpoxyController<Event>(
        modelBuildingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
) {

    override fun buildItemModel(currentPosition: Int, item: Event?): EpoxyModel<*> {
        return if (item == null) {
            LoadingItemModel_().id(-currentPosition)
        } else {
            TimelineEventItem(item.eventId ?: "$currentPosition").id(currentPosition)
        }
    }

    init {
        isDebugLoggingEnabled = true
    }

    override fun onExceptionSwallowed(exception: RuntimeException) {
        throw exception
    }

}