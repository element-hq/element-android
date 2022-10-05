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
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class ParseDeviceUserAgentUseCase @Inject constructor() {

    fun execute(userAgent: String?): DeviceExtendedInfo {
        if (userAgent == null) return createUnknownUserAgent()

        return when {
            userAgent.contains(ANDROID_KEYWORD) -> parseAndroidUserAgent(userAgent)
            userAgent.contains(IOS_KEYWORD) -> parseIosUserAgent(userAgent)
            userAgent.contains(DESKTOP_KEYWORD) -> parseDesktopUserAgent(userAgent)
            userAgent.contains(WEB_KEYWORD) -> parseWebUserAgent(userAgent)
            else -> createUnknownUserAgent()
        }
    }

    private fun parseAndroidUserAgent(userAgent: String): DeviceExtendedInfo {
        val appName = userAgent.substringBefore("/")
        val appVersion = userAgent.substringAfter("/").substringBefore(" (")
        val deviceInfoSegments = userAgent.substringAfter("(").substringBeforeLast(")").split("; ")
        val deviceModel: String?
        val deviceOperatingSystem: String?
        if (deviceInfoSegments.firstOrNull() == "Linux") {
            val deviceOperatingSystemIndex = deviceInfoSegments.indexOfFirst { it.startsWith("Android") }
            deviceOperatingSystem = deviceInfoSegments.getOrNull(deviceOperatingSystemIndex)
            deviceModel = deviceInfoSegments.getOrNull(deviceOperatingSystemIndex + 1)
        } else {
            deviceModel = deviceInfoSegments.getOrNull(0)
            deviceOperatingSystem = deviceInfoSegments.getOrNull(1)
        }
        return DeviceExtendedInfo(
                deviceType = DeviceType.MOBILE,
                deviceModel = deviceModel,
                deviceOperatingSystem = deviceOperatingSystem,
                clientName = appName,
                clientVersion = appVersion
        )
    }

    private fun parseIosUserAgent(userAgent: String): DeviceExtendedInfo {
        val appName = userAgent.substringBefore("/")
        val appVersion = userAgent.substringAfter("/").substringBefore(" (")
        val deviceInfoSegments = userAgent.substringAfter("(").substringBeforeLast(")").split("; ")
        val deviceModel = deviceInfoSegments.getOrNull(0)
        val deviceOperatingSystem = deviceInfoSegments.getOrNull(1)
        return DeviceExtendedInfo(
                deviceType = DeviceType.MOBILE,
                deviceModel = deviceModel,
                deviceOperatingSystem = deviceOperatingSystem,
                clientName = appName,
                clientVersion = appVersion
        )
    }

    private fun parseDesktopUserAgent(userAgent: String): DeviceExtendedInfo {
        val browserSegments = userAgent.split(" ")
        val (browserName, browserVersion) = when {
            isFirefox(browserSegments) -> {
                Pair("Firefox", getBrowserVersion(browserSegments, "Firefox"))
            }
            isEdge(browserSegments) -> {
                Pair("Edge", getBrowserVersion(browserSegments, "Edge"))
            }
            isMobile(browserSegments) -> {
                when (val name = getMobileBrowserName(browserSegments)) {
                    null -> {
                        Pair(null, null)
                    }
                    "Safari" -> {
                        Pair(name, getBrowserVersion(browserSegments, "Version"))
                    }
                    else -> {
                        Pair(name, getBrowserVersion(browserSegments, name))
                    }
                }
            }
            isSafari(browserSegments) -> {
                Pair("Safari", getBrowserVersion(browserSegments, "Version"))
            }
            else -> {
                when (val name = getRegularBrowserName(browserSegments)) {
                    null -> {
                        Pair(null, null)
                    }
                    else -> {
                        Pair(name, getBrowserVersion(browserSegments, name))
                    }
                }
            }
        }

        val deviceOperatingSystemSegments = userAgent.substringAfter("(").substringBefore(")").split("; ")
        val deviceOperatingSystem = if (deviceOperatingSystemSegments.getOrNull(1)?.startsWith("Android").orFalse()) {
            deviceOperatingSystemSegments.getOrNull(1)
        } else {
            deviceOperatingSystemSegments.getOrNull(0)
        }
        return DeviceExtendedInfo(
                deviceType = DeviceType.DESKTOP,
                deviceModel = null,
                deviceOperatingSystem = deviceOperatingSystem,
                clientName = browserName,
                clientVersion = browserVersion,
        )
    }

    private fun parseWebUserAgent(userAgent: String): DeviceExtendedInfo {
        return parseDesktopUserAgent(userAgent).copy(
                deviceType = DeviceType.WEB
        )
    }

    private fun createUnknownUserAgent(): DeviceExtendedInfo {
        return DeviceExtendedInfo(DeviceType.UNKNOWN)
    }

    private fun isFirefox(browserSegments: List<String>): Boolean {
        return browserSegments.lastOrNull()?.startsWith("Firefox").orFalse()
    }

    private fun getBrowserVersion(browserSegments: List<String>, browserName: String): String? {
        // Chrome/104.0.3497.100 -> 104
        return browserSegments
                .find { it.startsWith(browserName) }
                ?.split("/")
                ?.getOrNull(1)
                ?.split(".")
                ?.firstOrNull()
    }

    private fun isEdge(browserSegments: List<String>): Boolean {
        return browserSegments.lastOrNull()?.startsWith("Edge").orFalse()
    }

    private fun isSafari(browserSegments: List<String>): Boolean {
        return browserSegments.lastOrNull()?.startsWith("Safari").orFalse() &&
                browserSegments.getOrNull(browserSegments.size - 2)?.startsWith("Version").orFalse()
    }

    private fun isMobile(browserSegments: List<String>): Boolean {
        return browserSegments.getOrNull(browserSegments.size - 2)?.startsWith("Mobile").orFalse()
    }

    private fun getMobileBrowserName(browserSegments: List<String>): String? {
        val possibleBrowserName = browserSegments.getOrNull(browserSegments.size - 3)?.split("/")?.firstOrNull()
        return if (possibleBrowserName == "Version") {
            "Safari"
        } else {
            possibleBrowserName
        }
    }

    private fun getRegularBrowserName(browserSegments: List<String>): String? {
        return browserSegments.getOrNull(browserSegments.size - 2)?.split("/")?.firstOrNull()
    }

    companion object {
        // Element dbg/1.5.0-dev (Xiaomi; Mi 9T; Android 11; RKQ1.200826.002 test-keys; Flavour GooglePlay; MatrixAndroidSdk2 1.5.0)
        // Legacy : Element/1.0.0 (Linux; U; Android 6.0.1; SM-A510F Build/MMB29; Flavour GPlay; MatrixAndroidSdk2 1.0)
        private const val ANDROID_KEYWORD = "; MatrixAndroidSdk2"

        // Element/1.8.21 (iPhone XS Max; iOS 15.2; Scale/3.00)
        private const val IOS_KEYWORD = "; iOS "

        // Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) ElementNightly/2022091301
        // Chrome/104.0.5112.102 Electron/20.1.1 Safari/537.36
        private const val DESKTOP_KEYWORD = " Electron/"

        // Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36
        private const val WEB_KEYWORD = "Mozilla/"
    }
}
