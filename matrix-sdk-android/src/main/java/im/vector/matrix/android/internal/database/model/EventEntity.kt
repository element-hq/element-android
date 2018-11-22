package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

internal open class EventEntity(var eventId: String = "",
                                var type: String = "",
                                var content: String = "",
                                var prevContent: String? = null,
                                var stateKey: String? = null,
                                var originServerTs: Long? = null,
                                var sender: String? = null,
                                var age: Long? = 0,
                                var redacts: String? = null,
                                var stateIndex: Int = 0,
                                var displayIndex: Int = 0
) : RealmObject() {

    companion object {
        const val DEFAULT_STATE_INDEX = Int.MIN_VALUE
    }

    @LinkingObjects("events")
    val chunk: RealmResults<ChunkEntity>? = null

    @LinkingObjects("untimelinedStateEvents")
    val room: RealmResults<RoomEntity>? = null

}