package im.vector.riotredesign.features.home.room.detail.timeline

import android.text.TextUtils
import im.vector.matrix.android.api.session.events.model.TimelineEvent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.riotredesign.R
import im.vector.riotredesign.core.resources.StringProvider


//TODO : complete with call membership events
class RoomMemberItemFactory(private val stringProvider: StringProvider) {

    fun create(event: TimelineEvent): NoticeItem? {
        val roomMember = event.roomMember ?: return null
        val noticeText = buildRoomMemberNotice(event) ?: return null
        return NoticeItem(noticeText, roomMember.avatarUrl, roomMember.displayName)
    }

    private fun buildRoomMemberNotice(event: TimelineEvent): String? {
        val eventContent: RoomMember? = event.root.content.toModel()
        val prevEventContent: RoomMember? = event.root.prevContent.toModel()
        val isMembershipEvent = prevEventContent?.membership != eventContent?.membership
        return if (isMembershipEvent) {
            buildMembershipNotice(event, eventContent, prevEventContent)
        } else {
            buildProfileNotice(event, eventContent, prevEventContent)
        }
    }

    private fun buildProfileNotice(event: TimelineEvent, eventContent: RoomMember?, prevEventContent: RoomMember?): String? {
        val displayText = StringBuilder()
        // Check display name has been changed
        if (!TextUtils.equals(eventContent?.displayName, prevEventContent?.displayName)) {
            val displayNameText = when {
                prevEventContent?.displayName.isNullOrEmpty() -> stringProvider.getString(R.string.notice_display_name_set, event.root.sender, eventContent?.displayName)
                eventContent?.displayName.isNullOrEmpty()     -> stringProvider.getString(R.string.notice_display_name_removed, event.root.sender, prevEventContent?.displayName)
                else                                          -> stringProvider.getString(R.string.notice_display_name_changed_from, event.root.sender, prevEventContent?.displayName, eventContent?.displayName)
            }
            displayText.append(displayNameText)
        }
        // Check whether the avatar has been changed
        if (!TextUtils.equals(eventContent?.avatarUrl, prevEventContent?.avatarUrl)) {
            val displayAvatarText = if (displayText.isNotEmpty()) {
                displayText.append(" ")
                stringProvider.getString(R.string.notice_avatar_changed_too)
            } else {
                stringProvider.getString(R.string.notice_avatar_url_changed, event.roomMember?.displayName)
            }
            displayText.append(displayAvatarText)
        }
        return displayText.toString()
    }

    private fun buildMembershipNotice(event: TimelineEvent, eventContent: RoomMember?, prevEventContent: RoomMember?): String? {
        val senderDisplayName = event.roomMember?.displayName ?: return null
        val targetDisplayName = eventContent?.displayName ?: event.root.sender
        return when {
            Membership.INVITE == eventContent?.membership -> {
                // TODO get userId
                val selfUserId: String = ""
                when {
                    eventContent.thirdPartyInvite != null -> stringProvider.getString(R.string.notice_room_third_party_registered_invite, targetDisplayName, eventContent.thirdPartyInvite?.displayName)
                    TextUtils.equals(event.root.stateKey, selfUserId)
                                                          -> stringProvider.getString(R.string.notice_room_invite_you, senderDisplayName)
                    event.root.stateKey.isNullOrEmpty()   -> stringProvider.getString(R.string.notice_room_invite_no_invitee, senderDisplayName)
                    else                                  -> stringProvider.getString(R.string.notice_room_invite, senderDisplayName, targetDisplayName)
                }
            }
            Membership.JOIN == eventContent?.membership   -> stringProvider.getString(R.string.notice_room_join, senderDisplayName)
            Membership.LEAVE == eventContent?.membership  -> // 2 cases here: this member may have left voluntarily or they may have been "left" by someone else ie. kicked
                return if (TextUtils.equals(event.root.sender, event.root.stateKey)) {
                    if (prevEventContent?.membership == Membership.INVITE) {
                        stringProvider.getString(R.string.notice_room_reject, senderDisplayName)
                    } else {
                        stringProvider.getString(R.string.notice_room_leave, senderDisplayName)
                    }
                } else if (prevEventContent?.membership == Membership.INVITE) {
                    stringProvider.getString(R.string.notice_room_withdraw, senderDisplayName, targetDisplayName)
                } else if (prevEventContent?.membership == Membership.JOIN) {
                    stringProvider.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
                } else if (prevEventContent?.membership == Membership.BAN) {
                    stringProvider.getString(R.string.notice_room_unban, senderDisplayName, targetDisplayName)
                } else {
                    null
                }
            Membership.BAN == eventContent?.membership    -> stringProvider.getString(R.string.notice_room_ban, senderDisplayName, targetDisplayName)
            Membership.KNOCK == eventContent?.membership  -> stringProvider.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
            else                                          -> null
        }
    }


}


