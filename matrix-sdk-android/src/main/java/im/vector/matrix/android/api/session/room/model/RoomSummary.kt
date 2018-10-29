package im.vector.matrix.android.api.session.room.model

data class RoomSummary(
        val roomId: String,
        val displayName: String = "",
        val topic: String = "",
        val avatarUrl: String = ""
)