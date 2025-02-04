/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
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
        val browserInfo = parseBrowserInfoFromDesktopUserAgent(userAgent)
        val operatingSystem = parseOperatingSystemFromDesktopUserAgent(userAgent)

        return DeviceExtendedInfo(
                deviceType = DeviceType.DESKTOP,
                deviceModel = null,
                deviceOperatingSystem = operatingSystem,
                clientName = browserInfo.name,
                clientVersion = browserInfo.version,
        )
    }

    private data class BrowserInfo(val name: String? = null, val version: String? = null)

    private fun parseBrowserInfoFromDesktopUserAgent(userAgent: String): BrowserInfo {
        val browserSegments = userAgent.split(" ")
        return when {
            isFirefox(browserSegments) -> {
                BrowserInfo(BROWSER_FIREFOX, getBrowserVersion(browserSegments, BROWSER_FIREFOX))
            }
            isEdge(browserSegments) -> {
                BrowserInfo(BROWSER_EDGE, getBrowserVersion(browserSegments, BROWSER_EDGE))
            }
            isMobile(browserSegments) -> {
                when (val name = getMobileBrowserName(browserSegments)) {
                    null -> {
                        BrowserInfo()
                    }
                    BROWSER_SAFARI -> {
                        BrowserInfo(name, getBrowserVersion(browserSegments, "Version"))
                    }
                    else -> {
                        BrowserInfo(name, getBrowserVersion(browserSegments, name))
                    }
                }
            }
            isSafari(browserSegments) -> {
                BrowserInfo(BROWSER_SAFARI, getBrowserVersion(browserSegments, "Version"))
            }
            else -> {
                when (val name = getRegularBrowserName(browserSegments)) {
                    null -> {
                        BrowserInfo()
                    }
                    else -> {
                        BrowserInfo(name, getBrowserVersion(browserSegments, name))
                    }
                }
            }
        }
    }

    private fun parseOperatingSystemFromDesktopUserAgent(userAgent: String): String? {
        val deviceOperatingSystemSegments = userAgent
                .substringAfter("(")
                .substringBefore(")")
                .split("; ")
        val firstSegment = deviceOperatingSystemSegments.getOrNull(0).orEmpty()
        val secondSegment = deviceOperatingSystemSegments.getOrNull(1).orEmpty()

        return when {
            // e.g. (Macintosh; Intel Mac OS X 10_15_7) => macOS
            firstSegment.startsWith(OPERATING_SYSTEM_MAC_KEYWORD) -> OPERATING_SYSTEM_MAC
            // e.g. (Windows NT 10.0; Win64; x64) => Windows
            firstSegment.startsWith(OPERATING_SYSTEM_WINDOWS_KEYWORD) -> OPERATING_SYSTEM_WINDOWS_KEYWORD
            // e.g. (iPad; CPU OS 8_4_1 like Mac OS X) => iOS
            firstSegment.startsWith(DEVICE_IPAD_KEYWORD) || firstSegment.startsWith(DEVICE_IPHONE_KEYWORD) -> OPERATING_SYSTEM_IOS
            // e.g. (Linux; Android 9; SM-G973U Build/PPR1.180610.011) => Android
            secondSegment.startsWith(OPERATING_SYSTEM_ANDROID_KEYWORD) -> OPERATING_SYSTEM_ANDROID_KEYWORD
            else -> null
        }
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
        return browserSegments.lastOrNull()?.startsWith(BROWSER_FIREFOX).orFalse()
    }

    private fun getBrowserVersion(browserSegments: List<String>, browserName: String): String? {
        // e.g Chrome/104.0.3497.100 -> 104.0.3497.100
        return browserSegments
                .find { it.startsWith(browserName) }
                ?.split("/")
                ?.getOrNull(1)
    }

    private fun isEdge(browserSegments: List<String>): Boolean {
        return browserSegments.lastOrNull()?.startsWith(BROWSER_EDGE).orFalse()
    }

    private fun isSafari(browserSegments: List<String>): Boolean {
        return browserSegments.lastOrNull()?.startsWith(BROWSER_SAFARI).orFalse() &&
                browserSegments.getOrNull(browserSegments.size - 2)?.startsWith("Version").orFalse()
    }

    private fun isMobile(browserSegments: List<String>): Boolean {
        return browserSegments.getOrNull(browserSegments.size - 2)?.startsWith("Mobile").orFalse()
    }

    private fun getMobileBrowserName(browserSegments: List<String>): String? {
        val possibleBrowserName = browserSegments.getOrNull(browserSegments.size - 3)?.split("/")?.firstOrNull()
        return if (possibleBrowserName == "Version") {
            BROWSER_SAFARI
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

        private const val OPERATING_SYSTEM_MAC_KEYWORD = "Macintosh"
        private const val OPERATING_SYSTEM_MAC = "macOS"
        private const val OPERATING_SYSTEM_IOS = "iOS"
        private const val OPERATING_SYSTEM_WINDOWS_KEYWORD = "Windows"
        private const val OPERATING_SYSTEM_ANDROID_KEYWORD = "Android"
        private const val DEVICE_IPAD_KEYWORD = "iPad"
        private const val DEVICE_IPHONE_KEYWORD = "iPhone"

        private const val BROWSER_FIREFOX = "Firefox"
        private const val BROWSER_SAFARI = "Safari"
        private const val BROWSER_EDGE = "Edge"
    }
}
