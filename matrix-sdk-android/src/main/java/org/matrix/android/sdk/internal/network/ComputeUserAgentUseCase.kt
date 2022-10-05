/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.network

import android.content.Context
import android.os.Build
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.util.getApplicationInfoCompat
import org.matrix.android.sdk.api.util.getPackageInfoCompat
import javax.inject.Inject

class ComputeUserAgentUseCase @Inject constructor(
        private val context: Context,
) {

    /**
     * Create an user agent with the application version.
     * Ex: Element/1.5.0 (Xiaomi Mi 9T; Android 11; RKQ1.200826.002; Flavour GooglePlay; MatrixAndroidSdk2 1.5.0)
     *
     * @param flavorDescription the flavor description
     */
    fun execute(flavorDescription: String): String {
        val appPackageName = context.applicationContext.packageName
        val pm = context.packageManager

        val appName = tryOrNull { pm.getApplicationLabel(pm.getApplicationInfoCompat(appPackageName, 0)).toString() }
                ?.takeIf {
                    it.matches("\\A\\p{ASCII}*\\z".toRegex())
                }
                ?: run {
                    // Use appPackageName instead of appName if appName is null or contains any non-ASCII character
                    appPackageName
                }
        val appVersion = tryOrNull { pm.getPackageInfoCompat(context.applicationContext.packageName, 0).versionName } ?: FALLBACK_APP_VERSION

        val deviceManufacturer = Build.MANUFACTURER
        val deviceModel = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val deviceBuildId = Build.DISPLAY
        val matrixSdkVersion = BuildConfig.SDK_VERSION

        return buildString {
            append(appName)
            append("/")
            append(appVersion)
            append(" (")
            append(deviceManufacturer)
            append(" ")
            append(deviceModel)
            append("; ")
            append("Android ")
            append(androidVersion)
            append("; ")
            append(deviceBuildId)
            append("; ")
            append("Flavour ")
            append(flavorDescription)
            append("; ")
            append("MatrixAndroidSdk2 ")
            append(matrixSdkVersion)
            append(")")
        }
    }

    companion object {
        const val FALLBACK_APP_VERSION = "0.0.0"
    }
}
