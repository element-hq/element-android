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

import android.content.Context
import timber.log.Timber
import javax.inject.Inject

class AppNameProvider @Inject constructor(private val context: Context) {

    fun getAppName(): String {
        return try {
            val appPackageName = context.applicationContext.packageName
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(appPackageName, 0)
            var appName = pm.getApplicationLabel(appInfo).toString()

            // Use appPackageName instead of appName if appName contains any non-ASCII character
            if (!appName.matches("\\A\\p{ASCII}*\\z".toRegex())) {
                appName = appPackageName
            }
            appName
        } catch (e: Exception) {
            Timber.e(e, "## AppNameProvider() : failed")
            "ElementAndroid"
        }
    }
}
