/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.room

import android.content.Context
import im.vector.app.config.Config
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.provider.RoomDisplayNameFallbackProvider
import org.matrix.android.sdk.api.session.getRoom
import javax.inject.Inject
import javax.inject.Provider

class VectorRoomDisplayNameFallbackProvider @Inject constructor(
        private val context: Context,
        private val activeSessionHolder: Provider<ActiveSessionHolder>,
) : RoomDisplayNameFallbackProvider {

    override fun excludedUserIds(roomId: String): List<String> {
        if (!Config.SUPPORT_FUNCTIONAL_MEMBERS) return emptyList()
        return activeSessionHolder.get()
                .getSafeActiveSession()
                ?.getRoom(roomId)
                ?.stateService()
                ?.getFunctionalMembers()
                .orEmpty()
    }

    override fun getNameForRoomInvite(): String {
        return context.getString(CommonStrings.room_displayname_room_invite)
    }

    override fun getNameForEmptyRoom(isDirect: Boolean, leftMemberNames: List<String>): String {
        return if (leftMemberNames.isEmpty()) {
            context.getString(CommonStrings.room_displayname_empty_room)
        } else {
            val was = when (val size = leftMemberNames.size) {
                1 -> getNameFor1member(leftMemberNames[0])
                2 -> getNameFor2members(leftMemberNames[0], leftMemberNames[1])
                3 -> getNameFor3members(leftMemberNames[0], leftMemberNames[1], leftMemberNames[2])
                4 -> getNameFor4members(leftMemberNames[0], leftMemberNames[1], leftMemberNames[2], leftMemberNames[3])
                else -> getNameFor4membersAndMore(leftMemberNames[0], leftMemberNames[1], leftMemberNames[2], size - 3)
            }
            context.getString(CommonStrings.room_displayname_empty_room_was, was)
        }
    }

    override fun getNameFor1member(name: String) = name

    override fun getNameFor2members(name1: String, name2: String): String {
        return context.getString(CommonStrings.room_displayname_two_members, name1, name2)
    }

    override fun getNameFor3members(name1: String, name2: String, name3: String): String {
        return context.getString(CommonStrings.room_displayname_3_members, name1, name2, name3)
    }

    override fun getNameFor4members(name1: String, name2: String, name3: String, name4: String): String {
        return context.getString(CommonStrings.room_displayname_4_members, name1, name2, name3, name4)
    }

    override fun getNameFor4membersAndMore(name1: String, name2: String, name3: String, remainingCount: Int): String {
        return context.resources.getQuantityString(
                CommonPlurals.room_displayname_four_and_more_members,
                remainingCount,
                name1,
                name2,
                name3,
                remainingCount
        )
    }
}
