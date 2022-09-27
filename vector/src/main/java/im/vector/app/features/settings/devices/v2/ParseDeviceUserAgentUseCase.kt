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

package im.vector.app.features.settings.devices.v2

import im.vector.app.features.settings.devices.v2.list.DeviceType
import javax.inject.Inject

class ParseDeviceUserAgentUseCase @Inject constructor() {

    fun execute(userAgent: String): DeviceUserAgent {
        return when {
            userAgent.contains(ANDROID_KEYWORD) -> parseAndroidUserAgent(userAgent)
            userAgent.contains(IOS_KEYWORD) -> parseIosUserAgent(userAgent)
            userAgent.contains(DESKTOP_KEYWORD) -> parseDesktopUserAgent(userAgent)
            userAgent.contains(WEB_KEYWORD) -> parseWebUserAgent(userAgent)
            else -> createUnknownUserAgent()
        }
    }

    private fun parseAndroidUserAgent(userAgent: String): DeviceUserAgent {
        val appName = userAgent.substringBefore("/")
        val appVersion = userAgent.substringAfter("/").substringBefore(" (")
        val deviceInfoSegments = userAgent.substringAfter("(").substringBefore(")").split("; ")
        val deviceManufacturer = deviceInfoSegments.getOrNull(0)
        val deviceModel = deviceInfoSegments.getOrNull(1)
        val deviceOsInfo = deviceInfoSegments.getOrNull(2)?.takeIf { it.startsWith("Android") }
        val deviceOs = deviceOsInfo?.substringBefore(" ")
        val deviceOsVersion = deviceOsInfo?.substringAfter(" ")
        return DeviceUserAgent(DeviceType.MOBILE, deviceManufacturer, deviceModel, deviceOs, deviceOsVersion, appName, appVersion)
    }

    private fun parseIosUserAgent(userAgent: String): DeviceUserAgent {
        val appName = userAgent.substringBefore("/")
        val appVersion = userAgent.substringAfter("/").substringBefore(" (")
        val deviceInfoSegments = userAgent.substringAfter("(").substringBefore(")").split("; ")
        val deviceManufacturer = "Apple"
        val deviceModel = deviceInfoSegments.getOrNull(0)
        val deviceOs = deviceInfoSegments.getOrNull(1)?.substringBefore(" ")
        val deviceOsVersion = deviceInfoSegments.getOrNull(1)?.substringAfter(" ")
        return DeviceUserAgent(DeviceType.MOBILE, deviceManufacturer, deviceModel, deviceOs, deviceOsVersion, appName, appVersion)
    }

    private fun parseDesktopUserAgent(userAgent: String): DeviceUserAgent {
        val appInfoSegments = userAgent.substringBeforeLast(" ").substringAfterLast(" ").split("/")
        val appName = appInfoSegments.getOrNull(0)
        val appVersion = appInfoSegments.getOrNull(1)
        val deviceInfoSegments = userAgent.substringAfter("(").substringBefore(")").split("; ")
        val deviceOs = deviceInfoSegments.getOrNull(1)?.substringBeforeLast(" ")
        val deviceOsVersion = deviceInfoSegments.getOrNull(1)?.substringAfterLast(" ")
        return DeviceUserAgent(DeviceType.DESKTOP, null, null, deviceOs, deviceOsVersion, appName, appVersion)
    }

    private fun parseWebUserAgent(userAgent: String): DeviceUserAgent {
        return parseDesktopUserAgent(userAgent).copy(
                deviceType = DeviceType.WEB
        )
    }

    private fun createUnknownUserAgent(): DeviceUserAgent {
        return DeviceUserAgent(DeviceType.UNKNOWN)
    }

    companion object {
        // Element dbg/1.5.0-dev (Xiaomi; Mi 9T; Android 11; RKQ1.200826.002 test-keys; Flavour GooglePlay; MatrixAndroidSdk2 1.5.0)
        private val ANDROID_KEYWORD = "; MatrixAndroidSdk2"

        // Element/1.8.21 (iPhone XS Max; iOS 15.2; Scale/3.00)
        private val IOS_KEYWORD = "; iOS "

        // Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) ElementNightly/2022091301 Chrome/104.0.5112.102 Electron/20.1.1 Safari/537.36
        private val DESKTOP_KEYWORD = " Electron/"

        // Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36
        private val WEB_KEYWORD = "Mozilla/"
    }
}
