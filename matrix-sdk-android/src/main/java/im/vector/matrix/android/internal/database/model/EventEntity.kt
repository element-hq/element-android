package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class EventEntity(@PrimaryKey var eventId: String = "",
                       var type: String = "",
                       var content: String = "",
                       var prevContent: String? = null,
                       var stateKey: String? = null
) : RealmObject() {

    @LinkingObjects("events")
    val chunk: RealmResults<ChunkEntity>? = null

}