package im.vector.riotredesign.features.home

import android.support.v7.util.DiffUtil
import im.vector.matrix.android.api.session.events.model.Event

class EventDiffUtilCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(p0: Event, p1: Event): Boolean {
        return p0.eventId == p1.eventId
    }

    override fun areContentsTheSame(p0: Event, p1: Event): Boolean {
        return p0 == p1
    }
}