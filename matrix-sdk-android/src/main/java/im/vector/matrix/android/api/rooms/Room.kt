package im.vector.matrix.android.api.rooms

import im.vector.matrix.android.api.rooms.timeline.EventTimeline

interface Room {

    fun timeline(): EventTimeline

}