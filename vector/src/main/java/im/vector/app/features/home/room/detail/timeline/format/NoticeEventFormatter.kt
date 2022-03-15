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
import im.vector.app.features.roomprofile.permissions.RoleFormatter
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.extensions.appendNl
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isThread
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
import org.matrix.android.sdk.api.session.room.model.RoomServerAclContent
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

class NoticeEventFormatter @Inject constructor(
        private val activeSessionDataSource: ActiveSessionDataSource,
        private val roomHistoryVisibilityFormatter: RoomHistoryVisibilityFormatter,
        private val roleFormatter: RoleFormatter,
        private val vectorPreferences: VectorPreferences,
        private val sp: StringProvider
) {

    private val currentUserId: String?
        get() = activeSessionDataSource.currentValue?.orNull()?.myUserId

    private fun Event.isSentByCurrentUser() = senderId != null && senderId == currentUserId

    fun format(timelineEvent: TimelineEvent, isDm: Boolean): CharSequence? {
        return when (val type = timelineEvent.root.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES         -> formatJoinRulesEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName, isDm)
            EventType.STATE_ROOM_CREATE             -> formatRoomCreateEvent(timelineEvent.root, isDm)
            EventType.STATE_ROOM_NAME               -> formatRoomNameEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_TOPIC              -> formatRoomTopicEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_AVATAR             -> formatRoomAvatarEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_MEMBER             -> formatRoomMemberEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName, isDm)
            EventType.STATE_ROOM_THIRD_PARTY_INVITE -> formatRoomThirdPartyInvite(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName, isDm)
            EventType.STATE_ROOM_ALIASES            -> formatRoomAliasesEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_CANONICAL_ALIAS    -> formatRoomCanonicalAliasEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_HISTORY_VISIBILITY ->
                formatRoomHistoryVisibilityEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName, isDm)
            EventType.STATE_ROOM_SERVER_ACL         -> formatRoomServerAclEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_GUEST_ACCESS       -> formatRoomGuestAccessEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName, isDm)
            EventType.STATE_ROOM_ENCRYPTION         -> formatRoomEncryptionEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_WIDGET,
            EventType.STATE_ROOM_WIDGET_LEGACY      -> formatWidgetEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.STATE_ROOM_TOMBSTONE          -> formatRoomTombstoneEvent(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName, isDm)
            EventType.STATE_ROOM_POWER_LEVELS       -> formatRoomPowerLevels(timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.CALL_INVITE,
            EventType.CALL_CANDIDATES,
            EventType.CALL_HANGUP,
            EventType.CALL_REJECT,
            EventType.CALL_ANSWER                   -> formatCallEvent(type, timelineEvent.root, timelineEvent.senderInfo.disambiguatedDisplayName)
            EventType.CALL_NEGOTIATE,
            EventType.CALL_SELECT_ANSWER,
            EventType.CALL_REPLACES,
            EventType.MESSAGE,
            EventType.REACTION,
            EventType.KEY_VERIFICATION_START,
            EventType.KEY_VERIFICATION_CANCEL,
            EventType.KEY_VERIFICATION_ACCEPT,
            EventType.KEY_VERIFICATION_MAC,
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_KEY,
            EventType.KEY_VERIFICATION_READY,
            EventType.STATE_SPACE_CHILD,
            EventType.STATE_SPACE_PARENT,
            EventType.REDACTION,
            EventType.STICKER,
            in EventType.POLL_RESPONSE,
            in EventType.POLL_END                   -> formatDebug(timelineEvent.root)
            else                                    -> {
                Timber.v("Type $type not handled by this formatter")
                null
            }
        }
    }

    private fun formatRoomPowerLevels(event: Event, disambiguatedDisplayName: String): CharSequence? {
        val powerLevelsContent: PowerLevelsContent = event.content.toModel() ?: return null
        val previousPowerLevelsContent: PowerLevelsContent = event.resolvedPrevContent().toModel() ?: return null
        val userIds = HashSet<String>()
        userIds.addAll(powerLevelsContent.users.orEmpty().keys)
        userIds.addAll(previousPowerLevelsContent.users.orEmpty().keys)
        val diffs = ArrayList<String>()
        userIds.forEach { userId ->
            val from = PowerLevelsHelper(previousPowerLevelsContent).getUserRole(userId)
            val to = PowerLevelsHelper(powerLevelsContent).getUserRole(userId)
            if (from != to) {
                val fromStr = roleFormatter.format(from)
                val toStr = roleFormatter.format(to)
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
        val widgetContent: WidgetContent = event.content.toModel() ?: return null
        val previousWidgetContent: WidgetContent? = event.resolvedPrevContent().toModel()
        return if (widgetContent.isActive()) {
            val widgetName = widgetContent.getHumanName()
            if (previousWidgetContent?.isActive().orFalse()) {
                // Widget has been modified
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_widget_modified_by_you, widgetName)
                } else {
                    sp.getString(R.string.notice_widget_modified, disambiguatedDisplayName, widgetName)
                }
            } else {
                // Widget has been added
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_widget_added_by_you, widgetName)
                } else {
                    sp.getString(R.string.notice_widget_added, disambiguatedDisplayName, widgetName)
                }
            }
        } else {
            // Widget has been removed
            val widgetName = previousWidgetContent?.getHumanName()
            if (event.isSentByCurrentUser()) {
                sp.getString(R.string.notice_widget_removed_by_you, widgetName)
            } else {
                sp.getString(R.string.notice_widget_removed, disambiguatedDisplayName, widgetName)
            }
        }
    }

    fun format(event: Event, senderName: String?, isDm: Boolean): CharSequence? {
        return when (val type = event.getClearType()) {
            EventType.STATE_ROOM_JOIN_RULES         -> formatJoinRulesEvent(event, senderName, isDm)
            EventType.STATE_ROOM_NAME               -> formatRoomNameEvent(event, senderName)
            EventType.STATE_ROOM_TOPIC              -> formatRoomTopicEvent(event, senderName)
            EventType.STATE_ROOM_AVATAR             -> formatRoomAvatarEvent(event, senderName)
            EventType.STATE_ROOM_MEMBER             -> formatRoomMemberEvent(event, senderName, isDm)
            EventType.STATE_ROOM_THIRD_PARTY_INVITE -> formatRoomThirdPartyInvite(event, senderName, isDm)
            EventType.STATE_ROOM_HISTORY_VISIBILITY -> formatRoomHistoryVisibilityEvent(event, senderName, isDm)
            EventType.CALL_INVITE,
            EventType.CALL_HANGUP,
            EventType.CALL_REJECT,
            EventType.CALL_ANSWER                   -> formatCallEvent(type, event, senderName)
            EventType.STATE_ROOM_TOMBSTONE          -> formatRoomTombstoneEvent(event, senderName, isDm)
            else                                    -> {
                Timber.v("Type $type not handled by this formatter")
                null
            }
        }
    }

    private fun formatDebug(event: Event): CharSequence {
        val threadPrefix = if (event.isThread()) "thread" else ""
        return "Debug: $threadPrefix event type \"${event.getClearType()}\""
    }

    private fun formatRoomCreateEvent(event: Event, isDm: Boolean): CharSequence? {
        return event.content.toModel<RoomCreateContent>()
                ?.takeIf { it.creator.isNullOrBlank().not() }
                ?.let {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(if (isDm) R.string.notice_direct_room_created_by_you else R.string.notice_room_created_by_you)
                    } else {
                        sp.getString(if (isDm) R.string.notice_direct_room_created else R.string.notice_room_created, it.creator)
                    }
                }
    }

    private fun formatRoomNameEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.content.toModel<RoomNameContent>() ?: return null
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

    private fun formatRoomTombstoneEvent(event: Event, senderName: String?, isDm: Boolean): CharSequence? {
        return if (event.isSentByCurrentUser()) {
            sp.getString(if (isDm) R.string.notice_direct_room_update_by_you else R.string.notice_room_update_by_you)
        } else {
            sp.getString(if (isDm) R.string.notice_direct_room_update else R.string.notice_room_update, senderName)
        }
    }

    private fun formatRoomTopicEvent(event: Event, senderName: String?): CharSequence? {
        val content = event.content.toModel<RoomTopicContent>() ?: return null
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
        val content = event.content.toModel<RoomAvatarContent>() ?: return null
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

    private fun formatRoomHistoryVisibilityEvent(event: Event, senderName: String?, isDm: Boolean): CharSequence? {
        val historyVisibility = event.content.toModel<RoomHistoryVisibilityContent>()?.historyVisibility ?: return null

        val historyVisibilitySuffix = roomHistoryVisibilityFormatter.getNoticeSuffix(historyVisibility)
        return if (event.isSentByCurrentUser()) {
            sp.getString(if (isDm) R.string.notice_made_future_direct_room_visibility_by_you else R.string.notice_made_future_room_visibility_by_you,
                    historyVisibilitySuffix)
        } else {
            sp.getString(if (isDm) R.string.notice_made_future_direct_room_visibility else R.string.notice_made_future_room_visibility,
                    senderName, historyVisibilitySuffix)
        }
    }

    private fun formatRoomThirdPartyInvite(event: Event, senderName: String?, isDm: Boolean): CharSequence? {
        val content = event.content.toModel<RoomThirdPartyInviteContent>()
        val prevContent = event.resolvedPrevContent()?.toModel<RoomThirdPartyInviteContent>()

        return when {
            prevContent != null -> {
                // Revoke case
                if (event.isSentByCurrentUser()) {
                    sp.getString(
                            if (isDm) {
                                R.string.notice_direct_room_third_party_revoked_invite_by_you
                            } else {
                                R.string.notice_room_third_party_revoked_invite_by_you
                            },
                            prevContent.displayName)
                } else {
                    sp.getString(if (isDm) R.string.notice_direct_room_third_party_revoked_invite else R.string.notice_room_third_party_revoked_invite,
                            senderName, prevContent.displayName)
                }
            }
            content != null     -> {
                // Invitation case
                if (event.isSentByCurrentUser()) {
                    sp.getString(if (isDm) R.string.notice_direct_room_third_party_invite_by_you else R.string.notice_room_third_party_invite_by_you,
                            content.displayName)
                } else {
                    sp.getString(if (isDm) R.string.notice_direct_room_third_party_invite else R.string.notice_room_third_party_invite,
                            senderName, content.displayName)
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
            EventType.CALL_REJECT     ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.call_tile_you_declined_this_call)
                } else {
                    sp.getString(R.string.call_tile_other_declined, senderName)
                }
            else                      -> null
        }
    }

    private fun formatRoomMemberEvent(event: Event, senderName: String?, isDm: Boolean): String? {
        val eventContent: RoomMemberContent? = event.content.toModel()
        val prevEventContent: RoomMemberContent? = event.resolvedPrevContent().toModel()
        val isMembershipEvent = prevEventContent?.membership != eventContent?.membership ||
                eventContent?.membership == Membership.LEAVE
        return if (isMembershipEvent) {
            buildMembershipNotice(event, senderName, eventContent, prevEventContent, isDm)
        } else {
            buildProfileNotice(event, senderName, eventContent, prevEventContent)
        }
    }

    private fun formatRoomAliasesEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomAliasesContent? = event.content.toModel()
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

    private fun formatRoomServerAclEvent(event: Event, senderName: String?): String? {
        val eventContent = event.content.toModel<RoomServerAclContent>() ?: return null
        val prevEventContent = event.resolvedPrevContent()?.toModel<RoomServerAclContent>()

        return buildString {
            // Title
            append(if (prevEventContent == null) {
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_server_acl_set_title_by_you)
                } else {
                    sp.getString(R.string.notice_room_server_acl_set_title, senderName)
                }
            } else {
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_server_acl_updated_title_by_you)
                } else {
                    sp.getString(R.string.notice_room_server_acl_updated_title, senderName)
                }
            })
            if (eventContent.allowList.isEmpty()) {
                // Special case for stuck room
                appendNl(sp.getString(R.string.notice_room_server_acl_allow_is_empty))
            } else if (vectorPreferences.developerMode()) {
                // Details, only in developer mode
                appendAclDetails(eventContent, prevEventContent)
            }
        }
    }

    private fun StringBuilder.appendAclDetails(eventContent: RoomServerAclContent, prevEventContent: RoomServerAclContent?) {
        if (prevEventContent == null) {
            eventContent.allowList.forEach { appendNl(sp.getString(R.string.notice_room_server_acl_set_allowed, it)) }
            eventContent.denyList.forEach { appendNl(sp.getString(R.string.notice_room_server_acl_set_banned, it)) }
            if (eventContent.allowIpLiterals) {
                appendNl(sp.getString(R.string.notice_room_server_acl_set_ip_literals_allowed))
            } else {
                appendNl(sp.getString(R.string.notice_room_server_acl_set_ip_literals_not_allowed))
            }
        } else {
            // Display only diff
            var hasChanged = false
            // New allowed servers
            (eventContent.allowList - prevEventContent.allowList)
                    .also { hasChanged = hasChanged || it.isNotEmpty() }
                    .forEach { appendNl(sp.getString(R.string.notice_room_server_acl_updated_allowed, it)) }
            // Removed allowed servers
            (prevEventContent.allowList - eventContent.allowList)
                    .also { hasChanged = hasChanged || it.isNotEmpty() }
                    .forEach { appendNl(sp.getString(R.string.notice_room_server_acl_updated_was_allowed, it)) }
            // New denied servers
            (eventContent.denyList - prevEventContent.denyList)
                    .also { hasChanged = hasChanged || it.isNotEmpty() }
                    .forEach { appendNl(sp.getString(R.string.notice_room_server_acl_updated_banned, it)) }
            // Removed denied servers
            (prevEventContent.denyList - eventContent.denyList)
                    .also { hasChanged = hasChanged || it.isNotEmpty() }
                    .forEach { appendNl(sp.getString(R.string.notice_room_server_acl_updated_was_banned, it)) }

            if (prevEventContent.allowIpLiterals != eventContent.allowIpLiterals) {
                hasChanged = true
                if (eventContent.allowIpLiterals) {
                    appendNl(sp.getString(R.string.notice_room_server_acl_updated_ip_literals_allowed))
                } else {
                    appendNl(sp.getString(R.string.notice_room_server_acl_updated_ip_literals_not_allowed))
                }
            }

            if (!hasChanged) {
                appendNl(sp.getString(R.string.notice_room_server_acl_updated_no_change))
            }
        }
    }

    private fun formatRoomCanonicalAliasEvent(event: Event, senderName: String?): String? {
        val eventContent: RoomCanonicalAliasContent? = event.content.toModel()
        val prevContent: RoomCanonicalAliasContent? = event.resolvedPrevContent().toModel()
        val canonicalAlias = eventContent?.canonicalAlias?.takeIf { it.isNotEmpty() }
        val prevCanonicalAlias = prevContent?.canonicalAlias?.takeIf { it.isNotEmpty() }
        val altAliases = eventContent?.alternativeAliases.orEmpty()
        val prevAltAliases = prevContent?.alternativeAliases.orEmpty()
        val added = altAliases - prevAltAliases
        val removed = prevAltAliases - altAliases

        return when {
            added.isEmpty() && removed.isEmpty() && canonicalAlias == prevCanonicalAlias -> {
                // No difference between the two events say something as we can't simply hide the event from here
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_canonical_alias_no_change_by_you)
                } else {
                    sp.getString(R.string.notice_room_canonical_alias_no_change, senderName)
                }
            }
            added.isEmpty() && removed.isEmpty()                                         -> {
                // Canonical has changed
                if (canonicalAlias != null) {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_room_canonical_alias_set_by_you, canonicalAlias)
                    } else {
                        sp.getString(R.string.notice_room_canonical_alias_set, senderName, canonicalAlias)
                    }
                } else {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(R.string.notice_room_canonical_alias_unset_by_you)
                    } else {
                        sp.getString(R.string.notice_room_canonical_alias_unset, senderName)
                    }
                }
            }
            added.isEmpty() && canonicalAlias == prevCanonicalAlias                      -> {
                // Some alternative has been removed
                if (event.isSentByCurrentUser()) {
                    sp.getQuantityString(R.plurals.notice_room_canonical_alias_alternative_removed_by_you, removed.size, removed.joinToString())
                } else {
                    sp.getQuantityString(R.plurals.notice_room_canonical_alias_alternative_removed, removed.size, senderName, removed.joinToString())
                }
            }
            removed.isEmpty() && canonicalAlias == prevCanonicalAlias                    -> {
                // Some alternative has been added
                if (event.isSentByCurrentUser()) {
                    sp.getQuantityString(R.plurals.notice_room_canonical_alias_alternative_added_by_you, added.size, added.joinToString())
                } else {
                    sp.getQuantityString(R.plurals.notice_room_canonical_alias_alternative_added, added.size, senderName, added.joinToString())
                }
            }
            canonicalAlias == prevCanonicalAlias                                         -> {
                // Alternative added and removed
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_canonical_alias_alternative_changed_by_you)
                } else {
                    sp.getString(R.string.notice_room_canonical_alias_alternative_changed, senderName)
                }
            }
            else                                                                         -> {
                // Main and removed, or main and added, or main and added and removed
                if (event.isSentByCurrentUser()) {
                    sp.getString(R.string.notice_room_canonical_alias_main_and_alternative_changed_by_you)
                } else {
                    sp.getString(R.string.notice_room_canonical_alias_main_and_alternative_changed, senderName)
                }
            }
        }
    }

    private fun formatRoomGuestAccessEvent(event: Event, senderName: String?, isDm: Boolean): String? {
        val eventContent: RoomGuestAccessContent? = event.content.toModel()
        return when (eventContent?.guestAccess) {
            GuestAccess.CanJoin   ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(
                            if (isDm) R.string.notice_direct_room_guest_access_can_join_by_you else R.string.notice_room_guest_access_can_join_by_you
                    )
                } else {
                    sp.getString(if (isDm) R.string.notice_direct_room_guest_access_can_join else R.string.notice_room_guest_access_can_join,
                            senderName)
                }
            GuestAccess.Forbidden ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(
                            if (isDm) R.string.notice_direct_room_guest_access_forbidden_by_you else R.string.notice_room_guest_access_forbidden_by_you
                    )
                } else {
                    sp.getString(if (isDm) R.string.notice_direct_room_guest_access_forbidden else R.string.notice_room_guest_access_forbidden,
                            senderName)
                }
            else                  -> null
        }
    }

    private fun formatRoomEncryptionEvent(event: Event, senderName: String?): CharSequence? {
        if (!event.isStateEvent()) {
            return null
        }
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

    private fun buildMembershipNotice(event: Event,
                                      senderName: String?,
                                      eventContent: RoomMemberContent?,
                                      prevEventContent: RoomMemberContent?,
                                      isDm: Boolean): String? {
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
                eventContent.safeReason?.let { reason ->
                    if (event.isSentByCurrentUser()) {
                        sp.getString(if (isDm) R.string.notice_direct_room_join_with_reason_by_you else R.string.notice_room_join_with_reason_by_you,
                                reason)
                    } else {
                        sp.getString(if (isDm) R.string.notice_direct_room_join_with_reason else R.string.notice_room_join_with_reason,
                                senderDisplayName, reason)
                    }
                } ?: run {
                    if (event.isSentByCurrentUser()) {
                        sp.getString(if (isDm) R.string.notice_direct_room_join_by_you else R.string.notice_room_join_by_you)
                    } else {
                        sp.getString(if (isDm) R.string.notice_direct_room_join else R.string.notice_room_join,
                                senderDisplayName)
                    }
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
                            eventContent.safeReason?.let { reason ->
                                if (event.isSentByCurrentUser()) {
                                    sp.getString(
                                            if (isDm) {
                                                R.string.notice_direct_room_leave_with_reason_by_you
                                            } else {
                                                R.string.notice_room_leave_with_reason_by_you
                                            },
                                            reason
                                    )
                                } else {
                                    sp.getString(if (isDm) R.string.notice_direct_room_leave_with_reason else R.string.notice_room_leave_with_reason,
                                            senderDisplayName, reason)
                                }
                            } ?: run {
                                if (event.isSentByCurrentUser()) {
                                    sp.getString(if (isDm) R.string.notice_direct_room_leave_by_you else R.string.notice_room_leave_by_you)
                                } else {
                                    sp.getString(if (isDm) R.string.notice_direct_room_leave else R.string.notice_room_leave,
                                            senderDisplayName)
                                }
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
                                    sp.getString(R.string.notice_room_remove_with_reason_by_you, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_remove_by_you, targetDisplayName)
                            } else {
                                eventContent.safeReason?.let { reason ->
                                    sp.getString(R.string.notice_room_remove_with_reason, senderDisplayName, targetDisplayName, reason)
                                } ?: sp.getString(R.string.notice_room_remove, senderDisplayName, targetDisplayName)
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
                        sp.getString(R.string.notice_room_remove_with_reason_by_you, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_remove_by_you, targetDisplayName)
                } else {
                    eventContent.safeReason?.let { reason ->
                        sp.getString(R.string.notice_room_remove_with_reason, senderDisplayName, targetDisplayName, reason)
                    } ?: sp.getString(R.string.notice_room_remove, senderDisplayName, targetDisplayName)
                }
            else              -> null
        }
    }

    private fun formatJoinRulesEvent(event: Event, senderName: String?, isDm: Boolean): CharSequence? {
        val content = event.content.toModel<RoomJoinRulesContent>() ?: return null
        return when (content.joinRules) {
            RoomJoinRules.INVITE ->
                if (event.isSentByCurrentUser()) {
                    sp.getString(if (isDm) R.string.direct_room_join_rules_invite_by_you else R.string.room_join_rules_invite_by_you)
                } else {
                    sp.getString(if (isDm) R.string.direct_room_join_rules_invite else R.string.room_join_rules_invite,
                            senderName)
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
