/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.permissions

import androidx.annotation.StringRes
import im.vector.app.R
import org.matrix.android.sdk.api.session.events.model.EventType

/**
 * Change on each permission has an effect on the power level event. Try to sort the effect by category.
 */
sealed class EditablePermission(@StringRes val labelResId: Int, @StringRes val spaceLabelResId: Int = labelResId) {
    // Updates `content.events.[eventType]`
    open class EventTypeEditablePermission(val eventType: String,
                                           @StringRes labelResId: Int,
                                           @StringRes spaceLabelResId: Int = labelResId
    ) : EditablePermission(labelResId, spaceLabelResId)

    class ModifyWidgets : EventTypeEditablePermission(
            // Note: Element Web still use legacy value
            EventType.STATE_ROOM_WIDGET_LEGACY,
            R.string.room_permissions_modify_widgets
    )

    class ChangeRoomAvatar : EventTypeEditablePermission(
            EventType.STATE_ROOM_AVATAR,
            R.string.room_permissions_change_room_avatar,
            R.string.room_permissions_change_space_avatar
    )

    class ChangeMainAddressForTheRoom : EventTypeEditablePermission(
            EventType.STATE_ROOM_CANONICAL_ALIAS,
            R.string.room_permissions_change_main_address_for_the_room,
            R.string.room_permissions_change_main_address_for_the_space
    )

    class EnableRoomEncryption : EventTypeEditablePermission(
            EventType.STATE_ROOM_ENCRYPTION,
            R.string.room_permissions_enable_room_encryption,
            R.string.room_permissions_enable_space_encryption
    )

    class ChangeHistoryVisibility : EventTypeEditablePermission(
            EventType.STATE_ROOM_HISTORY_VISIBILITY,
            R.string.room_permissions_change_history_visibility
    )

    class ChangeRoomName : EventTypeEditablePermission(
            EventType.STATE_ROOM_NAME,
            R.string.room_permissions_change_room_name,
            R.string.room_permissions_change_space_name
    )

    class ChangePermissions : EventTypeEditablePermission(
            EventType.STATE_ROOM_POWER_LEVELS,
            R.string.room_permissions_change_permissions
    )

    class SendRoomServerAclEvents : EventTypeEditablePermission(
            EventType.STATE_ROOM_SERVER_ACL,
            R.string.room_permissions_send_m_room_server_acl_events
    )

    class UpgradeTheRoom : EventTypeEditablePermission(
            EventType.STATE_ROOM_TOMBSTONE,
            R.string.room_permissions_upgrade_the_room,
            R.string.room_permissions_upgrade_the_space
    )

    class ChangeTopic : EventTypeEditablePermission(
            EventType.STATE_ROOM_TOPIC,
            R.string.room_permissions_change_topic
    )

    // Updates `content.users_default`
    class DefaultRole : EditablePermission(R.string.room_permissions_default_role)

    // Updates `content.events_default`
    class SendMessages : EditablePermission(R.string.room_permissions_send_messages)

    // Updates `content.invites`
    class InviteUsers : EditablePermission(R.string.room_permissions_invite_users)

    // Updates `content.state_default`
    class ChangeSettings : EditablePermission(R.string.room_permissions_change_settings)

    // Updates `content.kick`
    class KickUsers : EditablePermission(R.string.room_permissions_remove_users)

    // Updates `content.ban`
    class BanUsers : EditablePermission(R.string.room_permissions_ban_users)

    // Updates `content.redact`
    class RemoveMessagesSentByOthers : EditablePermission(R.string.room_permissions_remove_messages_sent_by_others)

    // Updates `content.notification.room`
    class NotifyEveryone : EditablePermission(R.string.room_permissions_notify_everyone)
}
