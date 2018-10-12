package im.vector.matrix.android.internal.database.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany

@Entity
class RoomEntity {
    @Id var id: Long = 0
    lateinit var roomId: String
    lateinit var chunks: ToMany<ChunkEntity>
}