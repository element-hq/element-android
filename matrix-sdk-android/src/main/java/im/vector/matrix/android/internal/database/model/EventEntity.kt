package im.vector.matrix.android.internal.database.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class EventEntity {
    @Id var id: Long = 0
    lateinit var type: String
    lateinit var content: String
    var eventId: String? = null
    var prevContent: String? = null
    var stateKey: String? = null
}