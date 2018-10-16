package im.vector.matrix.android.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

// TODO to be completed
open class RoomSummaryEntity(@PrimaryKey var roomId: String = "",
                             var displayName: String? = "",
                             var topic: String? = "",
                             var lastMessage: EventEntity? = null
) : RealmObject()