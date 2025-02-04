/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import android.content.Context
import im.vector.app.core.utils.getApplicationLabel
import timber.log.Timber
import javax.inject.Inject

interface AppNameProvider {

    fun getAppName(): String
}

class DefaultAppNameProvider @Inject constructor(private val context: Context) : AppNameProvider {

    override fun getAppName(): String {
        return try {
            val appPackageName = context.applicationContext.packageName
            var appName = context.getApplicationLabel(appPackageName)

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
