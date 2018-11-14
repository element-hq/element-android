package im.vector.matrix.android.api.session.events.model

import im.vector.matrix.android.api.session.room.model.RoomMember

data class EnrichedEvent(val root: Event) {

    val metadata = HashMap<String, Any>()

    fun enrichWith(key: String?, data: Any?) {
        if (key == null || data == null) {
            return
        }
        if (!metadata.containsKey(key)) {
            metadata[key] = data
        }
    }

    inline fun <reified T> getMetadata(key: String): T? {
        return metadata[key] as T?
    }

    companion object {
        const val IS_LAST_EVENT = "IS_LAST_EVENT"
        const val READ_RECEIPTS = "READ_RECEIPTS"
    }

}

fun EnrichedEvent.roomMember(): RoomMember? {
    return getMetadata<RoomMember>(EventType.STATE_ROOM_MEMBER)
}
