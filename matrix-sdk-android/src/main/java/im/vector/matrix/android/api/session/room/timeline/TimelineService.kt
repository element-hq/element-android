package im.vector.matrix.android.api.session.room.timeline

import androidx.lifecycle.LiveData

interface TimelineService {

    fun timeline(eventId: String? = null): LiveData<TimelineData>

}