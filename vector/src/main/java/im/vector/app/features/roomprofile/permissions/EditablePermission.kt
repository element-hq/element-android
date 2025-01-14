/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import androidx.annotation.StringRes
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.events.model.EventType

/**
 * Change on each permission has an effect on the power level event. Try to sort the effect by category.
 */
sealed class EditablePermission(@StringRes val labelResId: Int, @StringRes val spaceLabelResId: Int = labelResId) {
    // Updates `content.events.[eventType]`
    open class EventTypeEditablePermission(
            val eventType: String,
            @StringRes labelResId: Int,
            @StringRes spaceLabelResId: Int = labelResId
    ) : EditablePermission(labelResId, spaceLabelResId)

    class ModifyWidgets : EventTypeEditablePermission(
            // Note: Element Web still use legacy value
            EventType.STATE_ROOM_WIDGET_LEGACY,
            CommonStrings.room_permissions_modify_widgets
    )

    class ChangeRoomAvatar : EventTypeEditablePermission(
            EventType.STATE_ROOM_AVATAR,
            CommonStrings.room_permissions_change_room_avatar,
            CommonStrings.room_permissions_change_space_avatar
    )

    class ChangeMainAddressForTheRoom : EventTypeEditablePermission(
            EventType.STATE_ROOM_CANONICAL_ALIAS,
            CommonStrings.room_permissions_change_main_address_for_the_room,
            CommonStrings.room_permissions_change_main_address_for_the_space
    )

    class EnableRoomEncryption : EventTypeEditablePermission(
            EventType.STATE_ROOM_ENCRYPTION,
            CommonStrings.room_permissions_enable_room_encryption,
            CommonStrings.room_permissions_enable_space_encryption
    )

    class ChangeHistoryVisibility : EventTypeEditablePermission(
            EventType.STATE_ROOM_HISTORY_VISIBILITY,
            CommonStrings.room_permissions_change_history_visibility
    )

    class ChangeRoomName : EventTypeEditablePermission(
            EventType.STATE_ROOM_NAME,
            CommonStrings.room_permissions_change_room_name,
            CommonStrings.room_permissions_change_space_name
    )

    class ChangePermissions : EventTypeEditablePermission(
            EventType.STATE_ROOM_POWER_LEVELS,
            CommonStrings.room_permissions_change_permissions
    )

    class SendRoomServerAclEvents : EventTypeEditablePermission(
            EventType.STATE_ROOM_SERVER_ACL,
            CommonStrings.room_permissions_send_m_room_server_acl_events
    )

    class UpgradeTheRoom : EventTypeEditablePermission(
            EventType.STATE_ROOM_TOMBSTONE,
            CommonStrings.room_permissions_upgrade_the_room,
            CommonStrings.room_permissions_upgrade_the_space
    )

    class ChangeTopic : EventTypeEditablePermission(
            EventType.STATE_ROOM_TOPIC,
            CommonStrings.room_permissions_change_topic
    )

    // Updates `content.users_default`
    class DefaultRole : EditablePermission(CommonStrings.room_permissions_default_role)

    // Updates `content.events_default`
    class SendMessages : EditablePermission(CommonStrings.room_permissions_send_messages)

    // Updates `content.invites`
    class InviteUsers : EditablePermission(CommonStrings.room_permissions_invite_users)

    // Updates `content.state_default`
    class ChangeSettings : EditablePermission(CommonStrings.room_permissions_change_settings)

    // Updates `content.kick`
    class KickUsers : EditablePermission(CommonStrings.room_permissions_remove_users)

    // Updates `content.ban`
    class BanUsers : EditablePermission(CommonStrings.room_permissions_ban_users)

    // Updates `content.redact`
    class RemoveMessagesSentByOthers : EditablePermission(CommonStrings.room_permissions_remove_messages_sent_by_others)

    // Updates `content.notification.room`
    class NotifyEveryone : EditablePermission(CommonStrings.room_permissions_notify_everyone)
}
