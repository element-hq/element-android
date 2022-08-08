/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.utils

import android.content.Context
import android.provider.Settings
import javax.inject.Inject

/**
 * A helper to get system settings.
 */
interface SystemSettingsProvider {

    /**
     * @return system setting for font scale
     */
    fun getSystemFontScale(): Float
}

class AndroidSystemSettingsProvider @Inject constructor(
        private val context: Context,
) : SystemSettingsProvider {

    override fun getSystemFontScale(): Float {
        return Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1f)
    }
}
