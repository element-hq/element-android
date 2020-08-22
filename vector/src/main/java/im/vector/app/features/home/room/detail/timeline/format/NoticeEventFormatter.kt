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

package im.vector.app.features.home.room.detail.timeline.format

import im.vector.app.ActiveSessionDataSource
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomAliasesContent
import org.matrix.android.sdk.api.session.room.model.RoomAvatarContent
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.model.RoomGuestAccessContent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.api.session.room.model.RoomThirdPartyInviteContent
import org.matrix.android.sdk.api.session.room.model.RoomTopicContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.widgets.model.WidgetContent
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.model.event.EncryptionEventContent
import timber.log.Timber
import javax.inject.Inject

class NoticeEventFormatter @Inject constructor(private val activeSessionDataSource: ActiveSessionDataSource,
                                               private val roomHistoryVisibilityFormatter: RoomHistoryVisibilityFormatter,
                                               private val sp: StringProvider) {

    private val currentUserId: String?
        get() = activeSessionDataSource.currentValue?.orNull()?.myUserId

    private fun Event.isSentByCurrentUser() = senderId != null && senderId == currentUserId

    fun format(timelineEvent: TimelineEvent): CharSequence? {
        return when (val type = timelineEvent.root.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES         -> formatJoinRulesEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_CREATE             -> formatRoomCreateEvent(timelineEvent.root)
            EventType.STATE_ROOM_NAME               -> formatRoomNameEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_TOPIC              -> formatRoomTopicEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_AVATAR             -> formatRoomAvatarEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_MEMBER             -> formatRoomMemberEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_THIRD_PARTY_INVITE -> formatRoomThirdPartyInvite(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_ALIASES            -> formatRoomAliasesEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_CANONICAL_ALIAS    -> formatRoomCanonicalAliasEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_GUEST_ACCESS       -> formatRoomGuestAccessEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_ENCRYPTION         -> formatRoomEncryptionEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_WIDGET,
            EventType.STATE_ROOM_WIDGET_LEGACY      -> formatWidgetEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_TOMBSTONE          -> formatRoomTombstoneEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_POWER_LEVELS       -> formatRoomPowerLevels(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.CALL_INVITE,
            EventType.CALL_CANDIDATES,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER                   -> formatCallEvent(type, timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
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

    private fun formatRoomPowerLevels(event: Event, disambiguatedDisplayName: String): CharSequence? {
        val powerLevelsContent: PowerLevelsContent = event.getClearContent().toModel() ?: return null
        val previousPowerLevelsContent: PowerLevelsContent = event.resolvedPrevContent().toModel() ?: return null
        val userIds = HashSet<String>()
        userIds.addAll(powerLevelsContent.users.keys)
        userIds.addAll(previousPowerLevelsContent.users.keys)
        val diffs = ArrayList<String>()
        userIds.forEach { userId ->
            val from = PowerLevelsHelper(previousPowerLevelsContent).getUserRole(userId)
            val to = PowerLevelsHelper(powerLevelsContent).getUserRole(userId)
            if (from != to) {
                val fromStr = sp.getString(from.res, from.value)
                val toStr = sp.getString(to.res, to.value)
                val diff = sp.getString(R.string.notice_power_level_diff, userId, fromStr, toStr)
                diffs.add(diff)
            }
        }
        if (diffs.isEmpty()) {
            return null
        }
        val diffStr = diffs.joinToString(separator = ", ")
        return if (event.isSentByCurrentUser()) {
            sp.getString(R.string.notice_power_level_changed_by_you, diffStr)
        } else {
            sp.getString(R.string.notice_power_level_changed, disambiguatedDisplayName, diffStr)
        }
    }

    private fun formatWidgetEvent(event: Event, disambiguatedDisplayName: String): CharSequence? {
        val widgetContent: WidgetContent = event.getClearContent().toModel() ?: return null
        val previousWidgetContent: WidgetContent? = event.resolvedPrevContent().toModel()
        return if (widgetContent.isActive()) {
            val widgetName = widgetContent.getHumanName()
            if (previousWidgetContent?.isActive().orFalse()) {
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_widget_modified_by_you, widgetName)
                } else {
                    sp.getString(R.string.notice_widget_modified, disambiguatedDisplayName, widgetName)
                }
            } else {
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_widget_added_by_you, widgetName)
                } else {
                    sp.getString(R.string.notice_widget_added, disambiguatedDisplayName, widgetName)
                }
            }
        } else {
            val widgetName = previousWidgetContent?.getHumanName()
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_widget_removed_by_you, widgetName)
            } else {
                sp.getString(R.string.notice_widget_removed, disambiguatedDisplayName, widgetName)
            }
        }
    }

    fun format(event: Event, senderName: String?): CharSequence? {
        return when (val type = event.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES         -> formatJoinRulesEvent(event, senderName)
            EventType.STATE_ROOM_NAME               -> formatRoomNameEvent(event, senderName)
            EventType.STATE_ROOM_TOPIC              -> formatRoomTopicEvent(event, senderName)
            EventType.STATE_ROOM_AVATAR             -> formatRoomAvatarEvent(event, senderName)
            EventType.STATE_ROOM_MEMBER             -> formatRoomMemberEvent(event, senderName)
            EventType.STATE_ROOM_THIRD_PARTY_INVITE -> formatRoomThirdPartyInvite(event, senderName)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(event, senderName)
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_ANSWER                   -> formatCallEvent(type, event, senderName)
            EventType.STATE_ROOM_TOMBSTONE          -> formatRoomTombstoneEvent(event, senderName)
            else                                    -> {
                Timber.v("Type $type not handled by this formatter")
                null
            }
        }
    }

    private fun formatDebug(event: Event): CharSequence? {
        return "{ \"type\": ${event.getClearType()} }"
    }

    private fun formatRoomCreateEvent(event: Event): CharSequence? {
        return event.getClearContent().toModel<RoomCreateContent>()
                ?.takeIf { it.creator.isNullOrBlank().not() }
                ?.let {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_room_created_by_you)
                    } else {
                        sp.getString(R.string.notice_room_created, it.creator)
                    }
                }
    }

    private fun formatRoomNameEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomNameContent>() ?: return null
        return if (content.name.isNullOrBlank()) {
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_room_name_removed_by_you)
            } else {
                sp.getString(R.string.notice_room_name_removed, senderName)
            }
        } else {
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_room_name_changed_by_you, content.name)
            } else {
                sp.getString(R.string.notice_room_name_changed, senderName, content.name)
            }
        }
    }

    private fun formatRoomTombstoneEvent(event: Event, senderName: String?): CharSequence? {
        return if (event.isSentByCurrentUser()) {
            sp.getString(R.string.notice_room_update_by_you)
        } else {
            sp.getString(R.string.notice_room_update, senderName)
        }
    }

    private fun formatRoomTopicEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomTopicContent>() ?: return null
        return if (content.topic.isNullOrEmpty()) {
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_room_topic_removed_by_you)
            } else {
                sp.getString(R.string.notice_room_topic_removed, senderName)
            }
        } else {
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_room_topic_changed_by_you, content.topic)
            } else {
                sp.getString(R.string.notice_room_topic_changed, senderName, content.topic)
            }
        }
    }

    private fun formatRoomAvatarEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomAvatarContent>() ?: return null
        return if (content.avatarUrl.isNullOrEmpty()) {
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_room_avatar_removed_by_you)
            } else {
                sp.getString(R.string.notice_room_avatar_removed, senderName)
            }
        } else {
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_room_avatar_changed_by_you)
            } else {
                sp.getString(R.string.notice_room_avatar_changed, senderName)
            }
        }
    }

    private fun formatRoomHistoryVisibilityEvent(event: Event, senderName: String?): CharSequence? {
        val historyVisibility = event.getClearContent().toModel<RoomHistoryVisibilityContent>()?.historyVisibility ?: return null

        val formattedVisibility = roomHistoryVisibilityFormatter.format(historyVisibility)
        return if (event.isSentByCurrentUser()) {
            sp.getString(R.string.notice_made_future_room_visibility_by_you, formattedVisibility)
        } else {
            sp.getString(R.string.notice_made_future_room_visibility, senderName, formattedVisibility)
        }
    }

    private fun formatRoomThirdPartyInvite(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomThirdPartyInviteContent>()
        val prevContent = event.resolvedPrevContent()?.toModel<RoomThirdPartyInviteContent>()

        return when {
            prevContent != null -> {
                // Revoke case
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_third_party_revoked_invite_by_you, prevContent.displayName)
                } else {
                    sp.getString(R.string.notice_room_third_party_revoked_invite, senderName, prevContent.displayName)
                }
            }
            content != null     -> {
                // Invitation case
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_third_party_invite_by_you, content.displayName)
                } else {
                    sp.getString(R.string.notice_room_third_party_invite, senderName, content.displayName)
                }
            }
            else                -> null
        }
    }

    private fun formatCallEvent(type: String, event: Event, senderName: String?): CharSequence? {
        return when (type) {
            EventType.CALL_INVITE     -> {
                val content = event.getClearContent().toModel<CallInviteContent>() ?: return null
                val isVideoCall = content.isVideo()
                return if (isVideoCall) {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_placed_video_call_by_you)
                    } else {
                        sp.getString(R.string.notice_placed_video_call, senderName)
                    }
                } else {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_placed_voice_call_by_you)
                    } else {
                        sp.getString(R.string.notice_placed_voice_call, senderName)
                    }
                }
            }
            EventType.CALL_ANSWER     ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_answered_call_by_you)
                } else {
                    sp.getString(R.string.notice_answered_call, senderName)
                }
            EventType.CALL_HANGUP     ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_ended_call_by_you)
                } else {
                    sp.getString(R.string.notice_ended_call, senderName)
                }
            EventType.CALL_CANDIDATES ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_call_candidates_by_you)
                } else {
                    sp.getString(R.string.notice_call_candidates, senderName)
                }
            else                      -> null
        }
    }

    private fun formatRoomMemberEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomMemberContent? = event.getClearContent().toModel()
        val prevEventContent: RoomMemberContent? = event.resolvedPrevContent().toModel()
        val isMembershipEvent = prevEventContent?.membership != eventContent?.membership
                || eventContent?.membership == Membership.LEAVE
        return if (isMembershipEvent) {
            buildMembershipNotice(event, senderName, eventContent, prevEventContent)
        } else {
            buildProfileNotice(event, senderName, eventContent, prevEventContent)
        }
    }

    private fun formatRoomAliasesEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomAliasesContent? = event.getClearContent().toModel()
        val prevEventContent: RoomAliasesContent? = event.resolvedPrevContent()?.toModel()

        val addedAliases = eventContent?.aliases.orEmpty() - prevEventContent?.aliases.orEmpty()
        val removedAliases = prevEventContent?.aliases.orEmpty() - eventContent?.aliases.orEmpty()

        return when {
            addedAliases.isNotEmpty() && removedAliases.isNotEmpty() ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_aliases_added_and_removed_by_you, addedAliases.joinToString(), removedAliases.joinToString())
                } else {
                    sp.getString(R.string.notice_room_aliases_added_and_removed, senderName, addedAliases.joinToString(), removedAliases.joinToString())
                }
            addedAliases.isNotEmpty()                                ->
                if (event.isSentByCurrentUser()) {
                    sp.getQuantityString(R.plurals.notice_room_aliases_added_by_you, addedAliases.size, addedAliases.joinToString())
                } else {
                    sp.getQuantityString(R.plurals.notice_room_aliases_added, addedAliases.size, senderName, addedAliases.joinToString())
                }
            removedAliases.isNotEmpty()                              ->
                if (event.isSentByCurrentUser()) {
                    sp.getQuantityString(R.plurals.notice_room_aliases_removed_by_you, removedAliases.size, removedAliases.joinToString())
                } else {
                    sp.getQuantityString(R.plurals.notice_room_aliases_removed, removedAliases.size, senderName, removedAliases.joinToString())
                }
            else                                                     -> {
                Timber.w("Alias event without any change...")
                null
            }
        }
    }

    private fun formatRoomCanonicalAliasEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomCanonicalAliasContent? = event.getClearContent().toModel()
        val canonicalAlias = eventContent?.canonicalAlias
        return canonicalAlias
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_room_canonical_alias_set_by_you, it)
                    } else {
                        sp.getString(R.string.notice_room_canonical_alias_set, senderName, it)
                    }
                }
                ?: if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_canonical_alias_unset_by_you)
                } else {
                    sp.getString(R.string.notice_room_canonical_alias_unset, senderName)
                }
    }

    private fun formatRoomGuestAccessEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomGuestAccessContent? = event.getClearContent().toModel()
        return when (eventContent?.guestAccess) {
            GuestAccess.CanJoin   ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_guest_access_can_join_by_you)
                } else {
                    sp.getString(R.string.notice_room_guest_access_can_join, senderName)
                }
            GuestAccess.Forbidden ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_guest_access_forbidden_by_you)
                } else {
                    sp.getString(R.string.notice_room_guest_access_forbidden, senderName)
                }
            else                  -> null
        }
    }

    private fun formatRoomEncryptionEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.content.toModel<EncryptionEventContent>() ?: return null
        return when (content.algorithm) {
            MXCRYPTO_ALGORITHM_MEGOLM ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_end_to_end_ok_by_you)
                } else {
                    sp.getString(R.string.notice_end_to_end_ok, senderName)
                }
            else                      ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_end_to_end_unknown_algorithm_by_you, content.algorithm)
                } else {
                    sp.getString(R.string.notice_end_to_end_unknown_algorithm, senderName, content.algorithm)
                }
        }
    }

    private fun buildProfileNotice(event: Event, senderName: String?, eventContent: RoomMemberContent?, prevEventContent: RoomMemberContent?): String {
        val displayText = StringBuilder()
        // Check display name has been changed
        if (eventContent?.displayName != prevEventContent?.displayName) {
            val displayNameText = when {
                prevEventContent?.displayName.isNullOrEmpty() ->
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_display_name_set_by_you, eventContent?.displayName)
                    } else {
                        sp.getString(R.string.notice_display_name_set, event.senderId, eventContent?.displayName)
                    }
                eventContent?.displayName.isNullOrEmpty()     ->
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_display_name_removed_by_you, prevEventContent?.displayName)
                    } else {
                        sp.getString(R.string.notice_display_name_removed, event.senderId, prevEventContent?.displayName)
                    }
                else                                          ->
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_display_name_changed_from_by_you, prevEventContent?.displayName, eventContent?.displayName)
                    } else {
                        sp.getString(R.string.notice_display_name_changed_from, event.senderId, prevEventContent?.displayName, eventContent?.displayName)
                    }
            }
            displayText.append(displayNameText)
        }
        // Check whether the avatar has been changed
        if (eventContent?.avatarUrl != prevEventContent?.avatarUrl) {
            val displayAvatarText = if (displayText.isNotEmpty()) {
                displayText.append(" ")
                sp.getString(R.string.notice_avatar_changed_too)
            } else {
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_avatar_url_changed_by_you)
                } else {
                    sp.getString(R.string.notice_avatar_url_changed, senderName)
                }
            }
            displayText.append(displayAvatarText)
        }
        if (displayText.isEmpty()) {
            displayText.append(
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_member_no_changes_by_you)
                    } else {
                        sp.getString(R.string.notice_member_no_changes, senderName)
                    }
            )
        }
        return displayText.toString()
    }

    private fun buildMembershipNotice(event: Event, senderName: String?, eventContent: RoomMemberContent?, prevEventContent: RoomMemberContent?): String? {
        val senderDisplayName = senderName ?: event.senderId ?: ""
        val targetDisplayName = eventContent?.displayName ?: prevEventContent?.displayName ?: event.stateKey ?: ""
        return when (eventContent?.membership) {
            Membership.INVITE -> {
                when {
                    eventContent.thirdPartyInvite != null -> {
                        val userWhoHasAccepted = eventContent.thirdPartyInvite?.signed?.mxid ?: event.stateKey
                        val threePidDisplayName = eventContent.thirdPartyInvite?.displayName ?: ""
                        eventContent.safeReason?.let { reason ->
                            if (event.isSentByCurrentUser()) {
                                sp.getString(R.string.notice_room_third_party_registered_invite_with_reason_by_you, threePidDisplayName, reason)
                            } else {
                                sp.getString(R.string.notice_room_third_party_registered_invite_with_reason, userWhoHasAccepted, threePidDisplayName, reason)
                            }
                        } ?: if (event.isSentByCurrentUser()) {
                            sp.getString(R.string.notice_room_third_party_registered_invite_by_you, threePidDisplayName)
                        } else {
                            sp.getString(R.string.notice_room_third_party_registered_invite, userWhoHasAccepted, threePidDisplayName)
                        }
                    }
                    event.stateKey == currentUserId       ->
                        eventContent.safeReason?.let { reason ->
                            sp.getString(R.string.notice_room_invite_you_with_reason, senderDisplayName, reason)
                        } ?: sp.getString(R.string.notice_room_invite_you, senderDisplayName)
                    event.stateKey.isNullOrEmpty()        ->
                        if (event.isSentByCurrentUser()) {
                            eventContent.safeReason?.let { reason ->
                                sp.getString(R.string.notice_room_invite_no_invitee_with_reason_by_you, reason)
                            } ?: sp.getString(R.string.notice_room_invite_no_invitee_by_you)
                        } else {
                            eventContent.safeReason?.let { reason ->
                                sp.getString(R.string.notice_room_invite_no_invitee_with_reason, senderDisplayName, reason)
                            } ?: sp.getString(R.string.notice_room_invite_no_invitee, senderDisplayName)
                        }
                    else                                  ->
                        if (event.isSentByCurrentUser()) {
                            eventContent.safeReason?.let { reason ->
                                sp.getString(R.string.notice_room_invite_with_reason_by_you, targetDisplayName, reason)
                            } ?: sp.getString(R.string.notice_room_invite_by_you, targetDisplayName)
                        } else {
                            eventContent.safeReason?.let { reason ->
                                sp.getString(R.string.notice_room_invite_with_reason, senderDisplayName, targetDisplayName, reason)
                            } ?: sp.getString(R.string.notice_room_invite, senderDisplayName, targetDisplayName)
                        }
                }
            }
            Membership.JOIN   ->
                if (event.isSentByCurrentUser()) {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_join_with_reason_by_you, reason)
                    } ?: sp.getString(R.string.notice_room_join_by_you)
                } else {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_join_with_reason, senderDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_join, senderDisplayName)
                }
            Membership.LEAVE  ->
                // 2 cases here: this member may have left voluntarily or they may have been "left" by someone else ie. kicked
                if (event.senderId == event.stateKey) {
                    when (prevEventContent?.membership) {
                        Membership.INVITE ->
                            if (event.isSentByCurrentUser()) {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_reject_with_reason_by_you, reason)
                                } ?: sp.getString(R.string.notice_room_reject_by_you)
                            } else {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_reject_with_reason, senderDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_reject, senderDisplayName)
                            }
                        else              ->
                            if (event.isSentByCurrentUser()) {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_leave_with_reason_by_you, reason)
                                } ?: sp.getString(R.string.notice_room_leave_by_you)
                            } else {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_leave_with_reason, senderDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_leave, senderDisplayName)
                            }
                    }
                } else {
                    when (prevEventContent?.membership) {
                        Membership.INVITE ->
                            if (event.isSentByCurrentUser()) {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_withdraw_with_reason_by_you, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_withdraw_by_you, targetDisplayName)
                            } else {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_withdraw_with_reason, senderDisplayName, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_withdraw, senderDisplayName, targetDisplayName)
                            }
                        Membership.LEAVE,
                        Membership.JOIN   ->
                            if (event.isSentByCurrentUser()) {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_kick_with_reason_by_you, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_kick_by_you, targetDisplayName)
                            } else {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_kick_with_reason, senderDisplayName, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
                            }
                        Membership.BAN    ->
                            if (event.isSentByCurrentUser()) {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_unban_with_reason_by_you, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_unban_by_you, targetDisplayName)
                            } else {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_unban_with_reason, senderDisplayName, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_unban, senderDisplayName, targetDisplayName)
                            }
                        else              -> null
                    }
                }
            Membership.BAN    ->
                if (event.isSentByCurrentUser()) {
                    eventContent.safeReason?.let {
                        sp.getString(R.string.notice_room_ban_with_reason_by_you, targetDisplayName, it)
                    } ?: sp.getString(R.string.notice_room_ban_by_you, targetDisplayName)
                } else {
                    eventContent.safeReason?.let {
                        sp.getString(R.string.notice_room_ban_with_reason, senderDisplayName, targetDisplayName, it)
                    } ?: sp.getString(R.string.notice_room_ban, senderDisplayName, targetDisplayName)
                }
            Membership.KNOCK  ->
                if (event.isSentByCurrentUser()) {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_kick_with_reason_by_you, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_kick_by_you, targetDisplayName)
                } else {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_kick_with_reason, senderDisplayName, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_kick, senderDisplayName, targetDisplayName)
                }
            else              -> null
        }
    }

    private fun formatJoinRulesEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.getClearContent().toModel<RoomJoinRulesContent>() ?: return null
        return when (content.joinRules) {
            RoomJoinRules.INVITE ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.room_join_rules_invite_by_you)
                } else {
                    sp.getString(R.string.room_join_rules_invite, senderName)
                }
            RoomJoinRules.PUBLIC ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.room_join_rules_public_by_you)
                } else {
                    sp.getString(R.string.room_join_rules_public, senderName)
                }
            else                 -> null
        }
    }

    fun formatRedactedEvent(event: Event): String {
        return (event
                .unsignedData
                ?.redactedEvent
                ?.content
                ?.get("reason") as? String)
                ?.takeIf { it.isNotBlank() }
                .let { reason ->
                    if (reason == null) {
                        if (event.isRedactedBySameUser()) {
                            sp.getString(R.string.event_redacted_by_user_reason)
                        } else {
                            sp.getString(R.string.event_redacted_by_admin_reason)
                        }
                    } else {
                        if (event.isRedactedBySameUser()) {
                            sp.getString(R.string.event_redacted_by_user_reason_with_reason, reason)
                        } else {
                            sp.getString(R.string.event_redacted_by_admin_reason_with_reason, reason)
                        }
                    }
                }
    }
}
