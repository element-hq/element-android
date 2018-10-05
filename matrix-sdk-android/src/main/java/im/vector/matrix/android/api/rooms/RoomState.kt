package im.vector.matrix.android.api.rooms

data class RoomState(
        var name: String? = null,
        var topic: String? = null,
        var url: String? = null,
        var avatar_url: String? = null,
        var join_rule: String? = null,
        var guest_access: String? = null,
        var history_visibility: RoomHistoryVisibility? = null,
        var visibility: RoomDirectoryVisibility? = null,
        var groups: List<String> = emptyList()
)