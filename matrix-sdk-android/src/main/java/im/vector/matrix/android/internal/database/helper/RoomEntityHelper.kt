package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity


internal fun RoomEntity.deleteOnCascade(chunkEntity: ChunkEntity) {
    chunks.remove(chunkEntity)
    chunkEntity.events.deleteAllFromRealm()
    chunkEntity.deleteFromRealm()
}
