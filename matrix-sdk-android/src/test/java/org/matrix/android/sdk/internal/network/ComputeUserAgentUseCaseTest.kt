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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.util.getApplicationInfoCompat
import org.matrix.android.sdk.api.util.getPackageInfoCompat

private const val A_PACKAGE_NAME = "org.matrix.sdk"
private const val AN_APP_NAME = "Element"
private const val A_NON_ASCII_APP_NAME = "Élement"
private const val AN_APP_VERSION = "1.5.1"
private const val A_FLAVOUR = "GooglePlay"

class ComputeUserAgentUseCaseTest {

    private val context = mockk<Context>()
    private val packageManager = mockk<PackageManager>()
    private val applicationInfo = mockk<ApplicationInfo>()
    private val packageInfo = mockk<PackageInfo>()

    private val computeUserAgentUseCase = ComputeUserAgentUseCase(context)

    @Before
    fun setUp() {
        every { context.applicationContext } returns context
        every { context.packageName } returns A_PACKAGE_NAME
        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfoCompat(any(), any()) } returns applicationInfo
        every { packageManager.getPackageInfoCompat(any(), any()) } returns packageInfo
    }

    @Test
    fun `given a non-null app name and app version when computing user agent then returns expected user agent`() {
        // Given
        givenAppName(AN_APP_NAME)
        givenAppVersion(AN_APP_VERSION)

        // When
        val result = computeUserAgentUseCase.execute(A_FLAVOUR)

        // Then
        val expectedUserAgent = constructExpectedUserAgent(AN_APP_NAME, AN_APP_VERSION)
        result shouldBeEqualTo expectedUserAgent
    }

    @Test
    fun `given a null app name when computing user agent then returns user agent with package name instead of app name`() {
        // Given
        givenAppName(null)
        givenAppVersion(AN_APP_VERSION)

        // When
        val result = computeUserAgentUseCase.execute(A_FLAVOUR)

        // Then
        val expectedUserAgent = constructExpectedUserAgent(A_PACKAGE_NAME, AN_APP_VERSION)
        result shouldBeEqualTo expectedUserAgent
    }

    @Test
    fun `given a non-ascii app name when computing user agent then returns user agent with package name instead of app name`() {
        // Given
        givenAppName(A_NON_ASCII_APP_NAME)
        givenAppVersion(AN_APP_VERSION)

        // When
        val result = computeUserAgentUseCase.execute(A_FLAVOUR)

        // Then
        val expectedUserAgent = constructExpectedUserAgent(A_PACKAGE_NAME, AN_APP_VERSION)
        result shouldBeEqualTo expectedUserAgent
    }

    @Test
    fun `given a null app version when computing user agent then returns user agent with a fallback app version`() {
        // Given
        givenAppName(AN_APP_NAME)
        givenAppVersion(null)

        // When
        val result = computeUserAgentUseCase.execute(A_FLAVOUR)

        // Then
        val expectedUserAgent = constructExpectedUserAgent(AN_APP_NAME, ComputeUserAgentUseCase.FALLBACK_APP_VERSION)
        result shouldBeEqualTo expectedUserAgent
    }

    private fun constructExpectedUserAgent(appName: String, appVersion: String): String {
        return buildString {
            append(appName)
            append("/")
            append(appVersion)
            append(" (")
            append(Build.MANUFACTURER)
            append(" ")
            append(Build.MODEL)
            append("; ")
            append("Android ")
            append(Build.VERSION.RELEASE)
            append("; ")
            append(Build.DISPLAY)
            append("; ")
            append("Flavour ")
            append(A_FLAVOUR)
            append("; ")
            append("MatrixAndroidSdk2 ")
            append(BuildConfig.SDK_VERSION)
            append(")")
        }
    }

    private fun givenAppName(deviceName: String?) {
        if (deviceName == null) {
            every { packageManager.getApplicationLabel(any()) } throws Exception("Cannot retrieve application name")
        } else if (!deviceName.matches("\\A\\p{ASCII}*\\z".toRegex())) {
            every { packageManager.getApplicationLabel(any()) } returns A_PACKAGE_NAME
        } else {
            every { packageManager.getApplicationLabel(any()) } returns deviceName
        }
    }

    private fun givenAppVersion(appVersion: String?) {
        packageInfo.versionName = appVersion
    }
}
