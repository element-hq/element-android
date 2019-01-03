package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.*

internal open class EventEntity(@PrimaryKey var localId: String = UUID.randomUUID().toString(),
                                var eventId: String = "",
                                var roomId: String = "",
                                var type: String = "",
                                var content: String? = null,
                                var prevContent: String? = null,
                                var stateKey: String? = null,
                                var originServerTs: Long? = null,
                                var sender: String? = null,
                                var age: Long? = 0,
                                var redacts: String? = null,
                                var stateIndex: Int = 0,
                                var displayIndex: Int = 0,
                                var isUnlinked: Boolean = false
) : RealmObject() {

    enum class LinkFilterMode {
        LINKED_ONLY,
        UNLINKED_ONLY,
        BOTH
    }

    companion object

    @LinkingObjects("events")
    val chunk: RealmResults<ChunkEntity>? = null

    @LinkingObjects("untimelinedStateEvents")
    val room: RealmResults<RoomEntity>? = null

}