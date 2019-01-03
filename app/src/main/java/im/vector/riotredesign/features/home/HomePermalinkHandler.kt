package im.vector.riotredesign.features.home

import android.net.Uri
import im.vector.matrix.android.api.permalinks.PermalinkData
import im.vector.matrix.android.api.permalinks.PermalinkParser

class HomePermalinkHandler(private val navigator: HomeNavigator) {

    fun launch(deepLink: String?) {
        val uri = deepLink?.let { Uri.parse(it) }
        launch(uri)
    }

    fun launch(deepLink: Uri?) {
        if (deepLink == null) {
            return
        }
        val permalinkData = PermalinkParser.parse(deepLink)
        when (permalinkData) {
            is PermalinkData.EventLink -> {
                navigator.openRoomDetail(permalinkData.roomIdOrAlias, permalinkData.eventId, true)
            }
            is PermalinkData.RoomLink -> {
                navigator.openRoomDetail(permalinkData.roomIdOrAlias, null, true)
            }
            is PermalinkData.GroupLink -> {
                navigator.openGroupDetail(permalinkData.groupId)
            }
            is PermalinkData.UserLink -> {
                navigator.openUserDetail(permalinkData.userId)
            }
            is PermalinkData.FallbackLink -> {

            }
        }
    }

}