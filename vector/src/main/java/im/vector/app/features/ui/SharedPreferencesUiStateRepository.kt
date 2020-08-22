/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.ui

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

/**
 * This class is used to persist UI state across application restart
 */
class SharedPreferencesUiStateRepository @Inject constructor(
        private val sharedPreferences: SharedPreferences,
        private val vectorPreferences: VectorPreferences
) : UiStateRepository {

    override fun reset() {
        sharedPreferences.edit {
            remove(KEY_DISPLAY_MODE)
        }
    }

    override fun getDisplayMode(): RoomListDisplayMode {
        return when (sharedPreferences.getInt(KEY_DISPLAY_MODE, VALUE_DISPLAY_MODE_CATCHUP)) {
            VALUE_DISPLAY_MODE_PEOPLE -> RoomListDisplayMode.PEOPLE
            VALUE_DISPLAY_MODE_ROOMS  -> RoomListDisplayMode.ROOMS
            else                      -> if (vectorPreferences.labAddNotificationTab()) {
                RoomListDisplayMode.NOTIFICATIONS
            } else {
                RoomListDisplayMode.PEOPLE
            }
        }
    }

    override fun storeDisplayMode(displayMode: RoomListDisplayMode) {
        sharedPreferences.edit {
            putInt(KEY_DISPLAY_MODE,
                    when (displayMode) {
                        RoomListDisplayMode.PEOPLE -> VALUE_DISPLAY_MODE_PEOPLE
                        RoomListDisplayMode.ROOMS  -> VALUE_DISPLAY_MODE_ROOMS
                        else                       -> VALUE_DISPLAY_MODE_CATCHUP
                    })
        }
    }

    companion object {
        private const val KEY_DISPLAY_MODE = "UI_STATE_DISPLAY_MODE"
        private const val VALUE_DISPLAY_MODE_CATCHUP = 0
        private const val VALUE_DISPLAY_MODE_PEOPLE = 1
        private const val VALUE_DISPLAY_MODE_ROOMS = 2
    }
}
