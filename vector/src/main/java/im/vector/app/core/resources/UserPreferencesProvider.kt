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
}
