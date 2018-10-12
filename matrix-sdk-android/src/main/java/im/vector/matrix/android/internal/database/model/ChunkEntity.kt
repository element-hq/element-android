package im.vector.matrix.android.internal.database.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany

@Entity
class ChunkEntity {
    @Id var id: Long = 0
    var prevToken: String? = null
    var nextToken: String? = null
    lateinit var events: ToMany<EventEntity>
}