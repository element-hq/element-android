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
        const val ROOM_MEMBER = "ROOM_MEMBER"
        const val IS_LAST_EVENT = "IS_LAST_EVENT"
        const val READ_RECEIPTS = "READ_RECEIPTS"
        const val LOCAL_ID = "LOCAL_ID"
    }

}

fun EnrichedEvent.roomMember(): RoomMember? {
    return getMetadata<RoomMember>(EnrichedEvent.ROOM_MEMBER)
}

fun EnrichedEvent.localId(): String? {
    return getMetadata<String>(EnrichedEvent.LOCAL_ID)
}
