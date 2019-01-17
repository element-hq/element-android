package im.vector.matrix.android.api.session.room.timeline

import androidx.paging.PagedList
import im.vector.matrix.android.api.session.events.model.TimelineEvent

data class TimelineData(
        val events: PagedList<TimelineEvent>,
        val isLoadingForward: Boolean = false,
        val isLoadingBackward: Boolean = false
)
