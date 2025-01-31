/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import android.content.Context
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.internal.di.MatrixScope
import timber.log.Timber
import javax.inject.Inject

@MatrixScope
internal class UserAgentHolder @Inject constructor(
        private val context: Context,
        matrixConfiguration: MatrixConfiguration
) {

    var userAgent: String = ""
        private set

    init {
        setApplicationFlavor(matrixConfiguration.applicationFlavor)
    }

    /**
     * Create an user agent with the application version.
     * Ex: Element/1.0.0 (Linux; U; Android 6.0.1; SM-A510F Build/MMB29; Flavour GPlay; MatrixAndroidSdk2 1.0)
     *
     * @param flavorDescription the flavor description
     */
    private fun setApplicationFlavor(flavorDescription: String) {
        var appName = ""
        var appVersion = ""

        try {
            val appPackageName = context.applicationContext.packageName
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(appPackageName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()

            val pkgInfo = pm.getPackageInfo(context.applicationContext.packageName, 0)
            appVersion = pkgInfo.versionName ?: ""

            // Use appPackageName instead of appName if appName contains any non-ASCII character
            if (!appName.matches("\\A\\p{ASCII}*\\z".toRegex())) {
                appName = appPackageName
            }
        } catch (e: Exception) {
            Timber.e(e, "## initUserAgent() : failed")
        }

        val systemUserAgent = System.getProperty("http.agent")

        // cannot retrieve the application version
        if (appName.isEmpty() || appVersion.isEmpty()) {
            if (null == systemUserAgent) {
                userAgent = "Java" + System.getProperty("java.version")
            }
            return
        }

        // if there is no user agent or cannot parse it
        if (null == systemUserAgent || systemUserAgent.lastIndexOf(")") == -1 || !systemUserAgent.contains("(")) {
            userAgent = (appName + "/" + appVersion + " ( Flavour " + flavorDescription +
                    "; MatrixAndroidSdk2 " + BuildConfig.SDK_VERSION + ")")
        } else {
            // update
            userAgent = appName + "/" + appVersion + " " +
                    systemUserAgent.substring(systemUserAgent.indexOf("("), systemUserAgent.lastIndexOf(")") - 1) +
                    "; Flavour " + flavorDescription +
                    "; MatrixAndroidSdk2 " + BuildConfig.SDK_VERSION + ")"
        }
    }
}
