/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.home.room.detail.timeline.format

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.*
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.resources.StringProvider
import timber.log.Timber
import javax.inject.Inject

class NoticeEventFormatter @Inject constructor(private val sessionHolder: ActiveSessionHolder,
                                               private val stringProvider: StringProvider) {

    fun format(timelineEvent: TimelineEvent): CharSequence? {
        return when (val type = timelineEvent.root.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES    -> formatJoinRulesEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_NAME          -> formatRoomNameEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_TOPIC         -> formatRoomTopicEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_MEMBER        -> formatRoomMemberEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_TOMBSTONE     -> formatRoomTombstoneEvent(timelineEvent.getDisambiguatedDisplayName())
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER              -> formatCallEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.MESSAGE,
            EventType.REACTION,
            EventType.REDACTION                -> formatDebug(timelineEvent.root)
            else                               -> {
                Timber.v("Type $type not handled by this formatter")
                null
            }
        }
    }

    fun format(event: Event, senderName: String?): CharSequence? {
        return when (val type = event.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES    -> formatJoinRulesEvent(event, senderName)
            EventType.STATE_ROOM_NAME          -> formatRoomNameEvent(event, senderName)
            EventType.STATE_ROOM_TOPIC         -> formatRoomTopicEvent(event, senderName)
            EventType.STATE_ROOM_MEMBER        -> formatRoomMemberEvent(event, senderName)
            EventType.STATE_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(event, senderName)
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER              -> formatCallEvent(event, senderName)
            EventType.STATE_ROOM_TOMBSTONE     -> formatRoomTombstoneEvent(senderName)
            else                               -> {
                Timber.v("Type $type not handled by this formatter")
                null
            }
        }
    }

    private fun formatDebug(event: Event): CharSequence? {
        return "{ \"type\": ${event.getClearType()} }"
    }

    private fun formatRoomNameEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomNameContent>() ?: return null
        return if (content.name.isNullOrBlank()) {
            stringProvider.getString(R.string.notice_room_name_removed, senderName)
        } else {
            stringProvider.getString(R.string.notice_room_name_changed, senderName, content.name)
        }
    }

    private fun formatRoomTombstoneEvent(senderName: String?): CharSequence? {
        return stringProvider.getString(R.string.notice_room_update, senderName)
    }

    private fun formatRoomTopicEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomTopicContent>() ?: return null
        return if (content.topic.isNullOrEmpty()) {
            stringProvider.getString(R.string.notice_room_topic_removed, senderName)
        } else {
            stringProvider.getString(R.string.notice_room_topic_changed, senderName, content.topic)
        }
    }

    private fun formatRoomHistoryVisibilityEvent(event: Event, senderName: String?): CharSequence? {
        val historyVisibility = event.getClearContent().toModel<RoomHistoryVisibilityContent>()?.historyVisibility ?: return null

        val formattedVisibility = when (historyVisibility) {
            RoomHistoryVisibility.SHARED         -> stringProvider.getString(R.string.notice_room_visibility_shared)
            RoomHistoryVisibility.INVITED        -> stringProvider.getString(R.string.notice_room_visibility_invited)
            RoomHistoryVisibility.JOINED         -> stringProvider.getString(R.string.notice_room_visibility_joined)
            RoomHistoryVisibility.WORLD_READABLE -> stringProvider.getString(R.string.notice_room_visibility_world_readable)
        }
        return stringProvider.getString(R.string.notice_made_future_room_visibility, senderName, formattedVisibility)
    }

    private fun formatCallEvent(event: Event, senderName: String?): CharSequence? {
        return when {
            EventType.CALL_INVITE == event.type -> {
                val content = event.getClearContent().toModel<CallInviteContent>() ?: return null
                val isVideoCall = content.offer.sdp == CallInviteContent.Offer.SDP_VIDEO
                return if (isVideoCall) {
                    stringProvider.getString(R.string.notice_placed_video_call, senderName)
                } else {
                    stringProvider.getString(R.string.notice_placed_voice_call, senderName)
                }
            }
            EventType.CALL_ANSWER == event.type -> stringProvider.getString(R.string.notice_answered_call, senderName)
            EventType.CALL_HANGUP == event.type -> stringProvider.getString(R.string.notice_ended_call, senderName)
            else                                -> null
        }
    }

    private fun formatRoomMemberEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomMember? = event.getClearContent().toModel()
        val prevEventContent: RoomMember? = event.prevContent.toModel()
        val isMembershipEvent = prevEventContent?.membership != eventContent?.membership
        return if (isMembershipEvent) {
            buildMembershipNotice(event, senderName, eventContent, prevEventContent)
        } else {
            buildProfileNotice(event, senderName, eventContent, prevEventContent)
        }
    }

    private fun buildProfileNotice(event: Event, senderName: String?, eventContent: RoomMember?, prevEventContent: RoomMember?): String {
        val displayText = StringBuilder()
        // Check display name has been changed
        if (eventContent?.displayName != prevEventContent?.displayName) {
            val displayNameText = when {
                prevEventContent?.displayName.isNullOrEmpty() ->
                    stringProvider.getString(R.string.notice_display_name_set, event.senderId, eventContent?.displayName)
                eventContent?.displayName.isNullOrEmpty()     ->
                    stringProvider.getString(R.string.notice_display_name_removed, event.senderId, prevEventContent?.displayName)
                else                                          ->
                    stringProvider.getString(R.string.notice_display_name_changed_from, event.senderId, prevEventContent?.displayName, eventContent?.displayName)
            }
            displayText.append(displayNameText)
        }
        // Check whether the avatar has been changed
        if (eventContent?.avatarUrl != prevEventContent?.avatarUrl) {
            val displayAvatarText = if (displayText.isNotEmpty()) {
                displayText.append(" ")
                stringProvider.getString(R.string.notice_avatar_changed_too)
            } else {
                stringProvider.getString(R.string.notice_avatar_url_changed, senderName)
            }
            displayText.append(displayAvatarText)
        }
        if (displayText.isEmpty()) {
            displayText.append(
                    stringProvider.getString(R.string.notice_member_no_changes, senderName)
            )
        }
        return displayText.toString()
    }

    private fun buildMembershipNotice(event: Event, senderName: String?, eventContent: RoomMember?, prevEventContent: RoomMember?): String? {
        val senderDisplayName = senderName ?: event.senderId
        val targetDisplayName = eventContent?.displayName ?: prevEventContent?.displayName ?: event.stateKey ?: ""
        return when {
            Membership.INVITE == eventContent?.membership -> {
                val selfUserId = sessionHolder.getSafeActiveSession()?.myUserId
                when {
                    eventContent.thirdPartyInvite != null -> {
                        val userWhoHasAccepted = eventContent.thirdPartyInvite?.signed?.mxid ?: event.stateKey
                        eventContent.safeReason
                                ?.let { reason -> stringProvider.getString(R.string.notice_room_third_party_registered_invite_with_reason, userWhoHasAccepted, eventContent.thirdPartyInvite?.displayName, reason) }
                                ?: stringProvider.getString(R.string.notice_room_third_party_registered_invite, userWhoHasAccepted, eventContent.thirdPartyInvite?.displayName)
                    }
                    event.stateKey == selfUserId          ->
                        eventContent.safeReason
                                ?.let { reason -> stringProvider.getString(R.string.notice_room_invite_you_with_reason, senderDisplayName, reason) }
                                ?: stringProvider.getString(R.string.notice_room_invite_you, senderDisplayName)
                    event.stateKey.isNullOrEmpty()        ->
                        eventContent.safeReason
                                ?.let { reason -> stringProvider.getString(R.string.notice_room_invite_no_invitee_with_reason, senderDisplayName, reason) }
                                ?: stringProvider.getString(R.string.notice_room_invite_no_invitee, senderDisplayName)
                    else                                  ->
                        eventContent.safeReason
                                ?.let { reason -> stringProvider.getString(R.string.notice_room_invite, senderDisplayName, targetDisplayName, reason) }
                                ?: stringProvider.getString(R.string.notice_room_invite, senderDisplayName, targetDisplayName)
                }
            }
            Membership.JOIN == eventContent?.membership   ->
                eventContent.safeReason
                        ?.let { reason -> stringProvider.getString(R.string.notice_room_join_with_reason, senderDisplayName, reason) }
                        ?: stringProvider.getString(R.string.notice_room_join, senderDisplayName)
            Membership.LEAVE == eventContent?.membership  ->
                // 2 cases here: this member may have left voluntarily or they may have been "left" by someone else ie. kicked
                return if (event.senderId == event.stateKey) {
                    if (prevEventContent?.membership == Membership.INVITE) {
                        eventContent.safeReason
                                ?.let { reason -> stringProvider.getString(R.string.notice_room_reject_with_reason, senderDisplayName, reason) }
                                ?: stringProvider.getString(R.string.notice_room_reject, senderDisplayName)
                    } else {
                        eventContent.safeReason
                                ?.let { reason -> stringProvider.getString(R.string.notice_room_leave_with_reason, senderDisplayName, reason) }
                                ?: stringProvider.getString(R.string.notice_room_leave, senderDisplayName)
                    }
                } else if (prevEventContent?.membership == Membership.INVITE) {
                    eventContent.safeReason
                            ?.let { reason -> stringProvider.getString(R.string.notice_room_withdraw_with_reason, senderDisplayName, targetDisplayName, reason) }
                            ?: stringProvider.getString(R.string.notice_room_withdraw, senderDisplayName, targetDisplayName)
                } else if (prevEventContent?.membership == Membership.JOIN) {
                    eventContent.safeReason
                            ?.let { reason -> stringProvider.getString(R.string.notice_room_kick_with_reason, senderDisplayName, targetDisplayName, reason) }
                            ?: stringProvider.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
                } else if (prevEventContent?.membership == Membership.BAN) {
                    eventContent.safeReason
                            ?.let { reason -> stringProvider.getString(R.string.notice_room_unban_with_reason, senderDisplayName, targetDisplayName, reason) }
                            ?: stringProvider.getString(R.string.notice_room_unban, senderDisplayName, targetDisplayName)
                } else {
                    null
                }
            Membership.BAN == eventContent?.membership    ->
                eventContent.safeReason
                        ?.let { reason -> stringProvider.getString(R.string.notice_room_ban_with_reason, senderDisplayName, targetDisplayName, reason) }
                        ?: stringProvider.getString(R.string.notice_room_ban, senderDisplayName, targetDisplayName)
            Membership.KNOCK == eventContent?.membership  ->
                eventContent.safeReason
                        ?.let { reason -> stringProvider.getString(R.string.notice_room_kick_with_reason, senderDisplayName, targetDisplayName, reason) }
                        ?: stringProvider.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
            else                                          -> null
        }
    }

    private fun formatJoinRulesEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomJoinRulesContent>() ?: return null
        return when (content.joinRules) {
            RoomJoinRules.INVITE -> stringProvider.getString(R.string.room_join_rules_invite, senderName)
            RoomJoinRules.PUBLIC -> stringProvider.getString(R.string.room_join_rules_public, senderName)
            else                 -> null
        }
    }
}
