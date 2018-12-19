package im.vector.matrix.android.api.permalinks

import android.net.Uri

sealed class PermalinkData {

    data class EventLink(val roomIdOrAlias: String, val eventId: String) : PermalinkData()

    data class RoomLink(val roomIdOrAlias: String) : PermalinkData()

    data class UserLink(val userId: String) : PermalinkData()

    data class GroupLink(val groupId: String) : PermalinkData()

    data class FallbackLink(val uri: Uri) : PermalinkData()

}
