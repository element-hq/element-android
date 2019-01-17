package im.vector.riotredesign.features.home.room.detail.timeline

import android.text.TextUtils
import im.vector.matrix.android.api.session.events.model.TimelineEvent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.RoomNameContent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.resources.StringProvider

class RoomNameItemFactory(private val stringProvider: StringProvider) {

    fun create(event: TimelineEvent): NoticeItem? {

        val content: RoomNameContent? = event.root.content.toModel()
        val roomMember = event.roomMember
        if (content == null || roomMember == null) {
            return null
        }
        val text = if (!TextUtils.isEmpty(content.name)) {
            stringProvider.getString(R.string.notice_room_name_changed, roomMember.displayName, content.name)
        } else {
            stringProvider.getString(R.string.notice_room_name_removed, roomMember.displayName)
        }
        return NoticeItem(text, roomMember.avatarUrl, roomMember.displayName)
    }


}