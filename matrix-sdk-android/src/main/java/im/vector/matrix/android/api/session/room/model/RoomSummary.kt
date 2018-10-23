package im.vector.matrix.android.api.session.room.model

data class RoomSummary(
        val roomId: String,
        var displayName: String = "",
        var topic: String = ""
)