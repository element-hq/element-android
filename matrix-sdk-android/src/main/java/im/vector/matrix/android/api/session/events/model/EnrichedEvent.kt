package im.vector.matrix.android.api.session.events.model

data class EnrichedEvent(val root: Event) {

    private val metaEventsByType = HashMap<String, ArrayList<Event>>()

    fun enrichWith(events: List<Event>) {
        events.forEach { enrichWith(it) }
    }

    fun enrichWith(event: Event?) {
        if (event == null) {
            return
        }
        var currentEventsForType = metaEventsByType[event.type]
        if (currentEventsForType == null) {
            currentEventsForType = ArrayList()
            metaEventsByType[event.type] = currentEventsForType
        }
        currentEventsForType.add(event)
    }

    fun getMetaEvents(type: String): List<Event> {
        return metaEventsByType[type] ?: emptyList()
    }

    fun getMetaEvents(): List<Event> {
        return metaEventsByType.values.flatten()
    }

    override fun toString(): String {
        return super.toString()
    }

}