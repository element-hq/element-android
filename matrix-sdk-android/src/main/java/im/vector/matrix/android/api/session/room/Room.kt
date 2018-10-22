package im.vector.matrix.android.api.session.room

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import im.vector.matrix.android.api.session.events.model.EnrichedEvent

interface Room {

    val roomId: String

    fun liveTimeline(): LiveData<PagedList<EnrichedEvent>>

}