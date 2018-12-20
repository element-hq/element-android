package im.vector.matrix.android.api.permalinks

import android.net.Uri
import im.vector.matrix.android.api.MatrixPatterns

object PermalinkParser {

    fun parse(uriString: String): PermalinkData {
        val uri = Uri.parse(uriString)
        return parse(uri)
    }

    fun parse(uri: Uri): PermalinkData {
        val fragment = uri.fragment
        if (fragment.isNullOrEmpty()) {
            return PermalinkData.FallbackLink(uri)
        }
        // we are limiting to 2 params
        val params = fragment
                .split(MatrixPatterns.SEP_REGEX.toRegex())
                .filter { it.isNotEmpty() }
                .take(2)

        val identifier = params.getOrNull(0)
        val extraParameter = params.getOrNull(1)
        if (identifier.isNullOrEmpty()) {
            return PermalinkData.FallbackLink(uri)
        }
        return when {
            MatrixPatterns.isUserId(identifier) -> PermalinkData.UserLink(userId = identifier)
            MatrixPatterns.isGroupId(identifier) -> PermalinkData.GroupLink(groupId = identifier)
            MatrixPatterns.isRoomId(identifier) -> {
                if (!extraParameter.isNullOrEmpty() && MatrixPatterns.isEventId(extraParameter)) {
                    PermalinkData.EventLink(roomIdOrAlias = identifier, eventId = extraParameter)
                } else {
                    PermalinkData.RoomLink(roomIdOrAlias = identifier)
                }
            }
            else -> PermalinkData.FallbackLink(uri)
        }
    }

}