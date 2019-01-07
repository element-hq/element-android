package im.vector.matrix.android.api.session.room.timeline

import android.arch.paging.PagedList
import im.vector.matrix.android.api.session.events.model.EnrichedEvent

data class TimelineData(
        val events: PagedList<EnrichedEvent>,
        val isLoadingForward: Boolean = false,
        val isLoadingBackward: Boolean = false
)
