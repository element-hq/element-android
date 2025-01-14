/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class UserPreferencesProvider @Inject constructor(private val vectorPreferences: VectorPreferences) {

    fun shouldShowHiddenEvents(): Boolean {
        return vectorPreferences.shouldShowHiddenEvents()
    }

    fun shouldShowReadReceipts(): Boolean {
        return vectorPreferences.showReadReceipts()
    }

    fun shouldShowRedactedMessages(): Boolean {
        return vectorPreferences.showRedactedMessages()
    }

    fun shouldShowLongClickOnRoomHelp(): Boolean {
        return vectorPreferences.shouldShowLongClickOnRoomHelp()
    }

    fun neverShowLongClickOnRoomHelpAgain() {
        vectorPreferences.neverShowLongClickOnRoomHelpAgain()
    }

    fun shouldShowJoinLeaves(): Boolean {
        return vectorPreferences.showJoinLeaveMessages()
    }

    fun shouldShowAvatarDisplayNameChanges(): Boolean {
        return vectorPreferences.showAvatarDisplayNameChangeMessages()
    }

    fun areThreadMessagesEnabled(): Boolean {
        return vectorPreferences.areThreadMessagesEnabled()
    }

    fun showLiveSenderInfo(): Boolean {
        return vectorPreferences.showLiveSenderInfo()
    }

    fun autoplayAnimatedImages(): Boolean {
        return vectorPreferences.autoplayAnimatedImages()
    }
}
