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
import im.vector.matrix.android.api.session.room.model.GuestAccess
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomAliasesContent
import im.vector.matrix.android.api.session.room.model.RoomCanonicalAliasContent
import im.vector.matrix.android.api.session.room.model.RoomGuestAccessContent
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibility
import im.vector.matrix.android.api.session.room.model.RoomHistoryVisibilityContent
import im.vector.matrix.android.api.session.room.model.RoomJoinRules
import im.vector.matrix.android.api.session.room.model.RoomJoinRulesContent
import im.vector.matrix.android.api.session.room.model.RoomMemberContent
import im.vector.matrix.android.api.session.room.model.RoomNameContent
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.api.session.room.model.call.CallInviteContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.model.event.EncryptionEventContent
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.resources.StringProvider
import timber.log.Timber
import javax.inject.Inject

class NoticeEventFormatter @Inject constructor(private val sessionHolder: ActiveSessionHolder,
                                               private val sp: StringProvider) {

    fun format(timelineEvent: TimelineEvent): CharSequence? {
        return when (val type = timelineEvent.root.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES         -> formatJoinRulesEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_NAME               -> formatRoomNameEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_TOPIC              -> formatRoomTopicEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_MEMBER             -> formatRoomMemberEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_ALIASES            -> formatRoomAliasesEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_CANONICAL_ALIAS    -> formatRoomCanonicalAliasEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_GUEST_ACCESS       -> formatRoomGuestAccessEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_ENCRYPTION         -> formatRoomEncryptionEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.STATE_ROOM_TOMBSTONE          -> formatRoomTombstoneEvent(timelineEvent.getDisambiguatedDisplayName())
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER                   -> formatCallEvent(timelineEvent.root, timelineEvent.getDisambiguatedDisplayName())
            EventType.MESSAGE,
            EventType.REACTION,
            EventType.KEY_VERIFICATION_START,
            EventType.KEY_VERIFICATION_CANCEL,
            EventType.KEY_VERIFICATION_ACCEPT,
            EventType.KEY_VERIFICATION_MAC,
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_KEY,
            EventType.KEY_VERIFICATION_READY,
            EventType.REDACTION                     -> formatDebug(timelineEvent.root)
            else                                    -> {
                Timber.v("Type $type not handled by this formatter")
                null
            }
        }
    }

    fun format(event: Event, senderName: String?): CharSequence? {
        return when (val type = event.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES         -> formatJoinRulesEvent(event, senderName)
            EventType.STATE_ROOM_NAME               -> formatRoomNameEvent(event, senderName)
            EventType.STATE_ROOM_TOPIC              -> formatRoomTopicEvent(event, senderName)
            EventType.STATE_ROOM_MEMBER             -> formatRoomMemberEvent(event, senderName)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(event, senderName)
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER                   -> formatCallEvent(event, senderName)
            EventType.STATE_ROOM_TOMBSTONE          -> formatRoomTombstoneEvent(senderName)
            else                                    -> {
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
            sp.getString(R.string.notice_room_name_removed, senderName)
        } else {
            sp.getString(R.string.notice_room_name_changed, senderName, content.name)
        }
    }

    private fun formatRoomTombstoneEvent(senderName: String?): CharSequence? {
        return sp.getString(R.string.notice_room_update, senderName)
    }

    private fun formatRoomTopicEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomTopicContent>() ?: return null
        return if (content.topic.isNullOrEmpty()) {
            sp.getString(R.string.notice_room_topic_removed, senderName)
        } else {
            sp.getString(R.string.notice_room_topic_changed, senderName, content.topic)
        }
    }

    private fun formatRoomHistoryVisibilityEvent(event: Event, senderName: String?): CharSequence? {
        val historyVisibility = event.getClearContent().toModel<RoomHistoryVisibilityContent>()?.historyVisibility ?: return null

        val formattedVisibility = when (historyVisibility) {
            RoomHistoryVisibility.SHARED         -> sp.getString(R.string.notice_room_visibility_shared)
            RoomHistoryVisibility.INVITED        -> sp.getString(R.string.notice_room_visibility_invited)
            RoomHistoryVisibility.JOINED         -> sp.getString(R.string.notice_room_visibility_joined)
            RoomHistoryVisibility.WORLD_READABLE -> sp.getString(R.string.notice_room_visibility_world_readable)
        }
        return sp.getString(R.string.notice_made_future_room_visibility, senderName, formattedVisibility)
    }

    private fun formatCallEvent(event: Event, senderName: String?): CharSequence? {
        return when {
            EventType.CALL_INVITE == event.type -> {
                val content = event.getClearContent().toModel<CallInviteContent>() ?: return null
                val isVideoCall = content.offer.sdp == CallInviteContent.Offer.SDP_VIDEO
                return if (isVideoCall) {
                    sp.getString(R.string.notice_placed_video_call, senderName)
                } else {
                    sp.getString(R.string.notice_placed_voice_call, senderName)
                }
            }
            EventType.CALL_ANSWER == event.type -> sp.getString(R.string.notice_answered_call, senderName)
            EventType.CALL_HANGUP == event.type -> sp.getString(R.string.notice_ended_call, senderName)
            else                                -> null
        }
    }

    private fun formatRoomMemberEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomMemberContent? = event.getClearContent().toModel()
        val prevEventContent: RoomMemberContent? = event.prevContent.toModel()
        val isMembershipEvent = prevEventContent?.membership != eventContent?.membership
        return if (isMembershipEvent) {
            buildMembershipNotice(event, senderName, eventContent, prevEventContent)
        } else {
            buildProfileNotice(event, senderName, eventContent, prevEventContent)
        }
    }

    private fun formatRoomAliasesEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomAliasesContent? = event.getClearContent().toModel()
        val prevEventContent: RoomAliasesContent? = event.unsignedData?.prevContent?.toModel()

        val addedAliases = eventContent?.aliases.orEmpty() - prevEventContent?.aliases.orEmpty()
        val removedAliases = prevEventContent?.aliases.orEmpty() - eventContent?.aliases.orEmpty()

        return if (addedAliases.isNotEmpty() && removedAliases.isNotEmpty()) {
            sp.getString(R.string.notice_room_aliases_added_and_removed, senderName, addedAliases.joinToString(), removedAliases.joinToString())
        } else if (addedAliases.isNotEmpty()) {
            sp.getQuantityString(R.plurals.notice_room_aliases_added, addedAliases.size, senderName, addedAliases.joinToString())
        } else if (removedAliases.isNotEmpty()) {
            sp.getQuantityString(R.plurals.notice_room_aliases_removed, removedAliases.size, senderName, removedAliases.joinToString())
        } else {
            Timber.w("Alias event without any change...")
            null
        }
    }

    private fun formatRoomCanonicalAliasEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomCanonicalAliasContent? = event.getClearContent().toModel()
        val canonicalAlias = eventContent?.canonicalAlias
        return canonicalAlias
                ?.takeIf { it.isNotBlank() }
                ?.let { sp.getString(R.string.notice_room_canonical_alias_set, senderName, it) }
                ?: sp.getString(R.string.notice_room_canonical_alias_unset, senderName)
    }

    private fun formatRoomGuestAccessEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomGuestAccessContent? = event.getClearContent().toModel()
        return when (eventContent?.guestAccess) {
            GuestAccess.CanJoin   -> sp.getString(R.string.notice_room_guest_access_can_join, senderName)
            GuestAccess.Forbidden -> sp.getString(R.string.notice_room_guest_access_forbidden, senderName)
            else                  -> null
        }
    }

    private fun formatRoomEncryptionEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.content.toModel<EncryptionEventContent>() ?: return null
        return if (content.algorithm == MXCRYPTO_ALGORITHM_MEGOLM) {
            sp.getString(R.string.notice_end_to_end_ok, senderName)
        } else {
            sp.getString(R.string.notice_end_to_end_unknown_algorithm, senderName, content.algorithm)
        }
    }

    private fun buildProfileNotice(event: Event, senderName: String?, eventContent: RoomMemberContent?, prevEventContent: RoomMemberContent?): String {
        val displayText = StringBuilder()
        // Check display name has been changed
        if (eventContent?.displayName != prevEventContent?.displayName) {
            val displayNameText = when {
                prevEventContent?.displayName.isNullOrEmpty() ->
                    sp.getString(R.string.notice_display_name_set, event.senderId, eventContent?.displayName)
                eventContent?.displayName.isNullOrEmpty()     ->
                    sp.getString(R.string.notice_display_name_removed, event.senderId, prevEventContent?.displayName)
                else                                          ->
                    sp.getString(R.string.notice_display_name_changed_from, event.senderId, prevEventContent?.displayName, eventContent?.displayName)
            }
            displayText.append(displayNameText)
        }
        // Check whether the avatar has been changed
        if (eventContent?.avatarUrl != prevEventContent?.avatarUrl) {
            val displayAvatarText = if (displayText.isNotEmpty()) {
                displayText.append(" ")
                sp.getString(R.string.notice_avatar_changed_too)
            } else {
                sp.getString(R.string.notice_avatar_url_changed, senderName)
            }
            displayText.append(displayAvatarText)
        }
        if (displayText.isEmpty()) {
            displayText.append(
                    sp.getString(R.string.notice_member_no_changes, senderName)
            )
        }
        return displayText.toString()
    }

    private fun buildMembershipNotice(event: Event, senderName: String?, eventContent: RoomMemberContent?, prevEventContent: RoomMemberContent?): String? {
        val senderDisplayName = senderName ?: event.senderId ?: ""
        val targetDisplayName = eventContent?.displayName ?: prevEventContent?.displayName ?: event.stateKey ?: ""
        return when (eventContent?.membership) {
            Membership.INVITE -> {
                val selfUserId = sessionHolder.getSafeActiveSession()?.myUserId
                when {
                    eventContent.thirdPartyInvite != null -> {
                        val userWhoHasAccepted = eventContent.thirdPartyInvite?.signed?.mxid ?: event.stateKey
                        val threePidDisplayName = eventContent.thirdPartyInvite?.displayName ?: ""
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_third_party_registered_invite_with_reason, userWhoHasAccepted, threePidDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_third_party_registered_invite, userWhoHasAccepted, threePidDisplayName)
                    }
                    event.stateKey == selfUserId          ->
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_invite_you_with_reason, senderDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_invite_you, senderDisplayName)
                    event.stateKey.isNullOrEmpty()        ->
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_invite_no_invitee_with_reason, senderDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_invite_no_invitee, senderDisplayName)
                    else                                  ->
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_invite_with_reason, senderDisplayName, targetDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_invite, senderDisplayName, targetDisplayName)
                }
            }
            Membership.JOIN   ->
                eventContent.safeReason?.let { reason ->
                    sp.getString(R.string.notice_room_join_with_reason, senderDisplayName, reason)
                } ?: sp.getString(R.string.notice_room_join, senderDisplayName)
            Membership.LEAVE  ->
                // 2 cases here: this member may have left voluntarily or they may have been "left" by someone else ie. kicked
                if (event.senderId == event.stateKey) {
                    if (prevEventContent?.membership == Membership.INVITE) {
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_reject_with_reason, senderDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_reject, senderDisplayName)
                    } else {
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_leave_with_reason, senderDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_leave, senderDisplayName)
                    }
                } else if (prevEventContent?.membership == Membership.INVITE) {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_withdraw_with_reason, senderDisplayName, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_withdraw, senderDisplayName, targetDisplayName)
                } else if (prevEventContent?.membership == Membership.JOIN) {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_kick_with_reason, senderDisplayName, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
                } else if (prevEventContent?.membership == Membership.BAN) {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_unban_with_reason, senderDisplayName, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_unban, senderDisplayName, targetDisplayName)
                } else {
                    null
                }
            Membership.BAN    ->
                eventContent.safeReason?.let {
                    sp.getString(R.string.notice_room_ban_with_reason, senderDisplayName, targetDisplayName, it)
                } ?: sp.getString(R.string.notice_room_ban, senderDisplayName, targetDisplayName)
            Membership.KNOCK  ->
                eventContent.safeReason?.let { reason ->
                    sp.getString(R.string.notice_room_kick_with_reason, senderDisplayName, targetDisplayName, reason)
                } ?: sp.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
            else              -> null
        }
    }

    private fun formatJoinRulesEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomJoinRulesContent>() ?: return null
        return when (content.joinRules) {
            RoomJoinRules.INVITE -> sp.getString(R.string.room_join_rules_invite, senderName)
            RoomJoinRules.PUBLIC -> sp.getString(R.string.room_join_rules_public, senderName)
            else                 -> null
        }
    }
}
