package im.vector.riotredesign.features.home

import android.support.v7.util.DiffUtil
import im.vector.matrix.android.api.session.events.model.EnrichedEvent

class EventDiffUtilCallback : DiffUtil.ItemCallback<EnrichedEvent>() {
    override fun areItemsTheSame(p0: EnrichedEvent, p1: EnrichedEvent): Boolean {
        return p0.root.eventId == p1.root.eventId
    }

    override fun areContentsTheSame(p0: EnrichedEvent, p1: EnrichedEvent): Boolean {
        return p0.root == p1.root
               && p0.getMetaEvents()
                       .zip(p1.getMetaEvents()) { a, b ->
                           a.eventId == b.eventId
                       }.none { !it }
    }
}