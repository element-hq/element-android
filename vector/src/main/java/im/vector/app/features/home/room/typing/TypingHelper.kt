/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.typing

import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import javax.inject.Inject

class TypingHelper @Inject constructor(private val stringProvider: StringProvider) {

    /**
     * Returns a human readable String of currently typing users in the room (excluding yourself).
     */
    fun getTypingMessage(typingUsers: List<SenderInfo>): String {
        return when {
            typingUsers.isEmpty() ->
                ""
            typingUsers.size == 1 ->
                stringProvider.getString(CommonStrings.room_one_user_is_typing, typingUsers[0].disambiguatedDisplayName)
            typingUsers.size == 2 ->
                stringProvider.getString(
                        CommonStrings.room_two_users_are_typing,
                        typingUsers[0].disambiguatedDisplayName,
                        typingUsers[1].disambiguatedDisplayName
                )
            else ->
                stringProvider.getString(
                        CommonStrings.room_many_users_are_typing,
                        typingUsers[0].disambiguatedDisplayName,
                        typingUsers[1].disambiguatedDisplayName
                )
        }
    }

    fun getNotificationTypingMessage(typingUsers: List<SenderInfo>): String {
        return when {
            typingUsers.isEmpty() -> ""
            typingUsers.size == 1 -> typingUsers[0].disambiguatedDisplayName
            typingUsers.size == 2 -> stringProvider.getString(
                    CommonStrings.room_notification_two_users_are_typing,
                    typingUsers[0].disambiguatedDisplayName, typingUsers[1].disambiguatedDisplayName
            )
            else -> stringProvider.getString(
                    CommonStrings.room_notification_more_than_two_users_are_typing,
                    typingUsers[0].disambiguatedDisplayName, typingUsers[1].disambiguatedDisplayName
            )
        }
    }
}
