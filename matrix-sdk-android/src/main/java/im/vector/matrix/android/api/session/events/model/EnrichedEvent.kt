package im.vector.matrix.android.api.session.events.model

import im.vector.matrix.android.api.session.room.model.RoomMember

data class EnrichedEvent(
        val root: Event,
        val localId: String,
        val roomMember: RoomMember?
) {

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
}
