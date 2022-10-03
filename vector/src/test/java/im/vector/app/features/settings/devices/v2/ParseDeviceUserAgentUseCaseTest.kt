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
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_USER_AGENT_LIST_FOR_ANDROID = listOf(
        // New User Agent Implementation
        "Element dbg/1.5.0-dev (Xiaomi Mi 9T; Android 11; RKQ1.200826.002 test-keys; Flavour GooglePlay; MatrixAndroidSdk2 1.5.2)",
        "Element/1.5.0 (Samsung SM-G960F; Android 6.0.1; RKQ1.200826.002; Flavour FDroid; MatrixAndroidSdk2 1.5.2)",
        "Element/1.5.0 (Google Nexus 5; Android 7.0; RKQ1.200826.002 test test; Flavour FDroid; MatrixAndroidSdk2 1.5.2)",
        "Element/1.5.0 (Google (Nexus) 5; Android 7.0; RKQ1.200826.002 test test; Flavour FDroid; MatrixAndroidSdk2 1.5.2)",
        "Element/1.5.0 (Google (Nexus) (5); Android 7.0; RKQ1.200826.002 test test; Flavour FDroid; MatrixAndroidSdk2 1.5.2)",
        // Legacy User Agent Implementation
        "Element/1.0.0 (Linux; U; Android 6.0.1; SM-A510F Build/MMB29; Flavour GPlay; MatrixAndroidSdk2 1.0)",
        "Element/1.0.0 (Linux; Android 7.0; SM-G610M Build/NRD90M; Flavour GPlay; MatrixAndroidSdk2 1.0)",
)
private val AN_EXPECTED_RESULT_LIST_FOR_ANDROID = listOf(
        DeviceExtendedInfo(DeviceType.MOBILE, "Xiaomi Mi 9T", "Android 11", "Element dbg", "1.5.0-dev"),
        DeviceExtendedInfo(DeviceType.MOBILE, "Samsung SM-G960F", "Android 6.0.1", "Element", "1.5.0"),
        DeviceExtendedInfo(DeviceType.MOBILE, "Google Nexus 5", "Android 7.0", "Element", "1.5.0"),
        DeviceExtendedInfo(DeviceType.MOBILE, "Google (Nexus) 5", "Android 7.0", "Element", "1.5.0"),
        DeviceExtendedInfo(DeviceType.MOBILE, "Google (Nexus) (5)", "Android 7.0", "Element", "1.5.0"),
        DeviceExtendedInfo(DeviceType.MOBILE, "SM-A510F Build/MMB29", "Android 6.0.1", "Element", "1.0.0"),
        DeviceExtendedInfo(DeviceType.MOBILE, "SM-G610M Build/NRD90M", "Android 7.0", "Element", "1.0.0"),
)

private val A_USER_AGENT_LIST_FOR_IOS = listOf(
        "Element/1.8.21 (iPhone; iOS 15.2; Scale/3.00)",
        "Element/1.8.21 (iPhone XS Max; iOS 15.2; Scale/3.00)",
        "Element/1.8.21 (iPad Pro (11-inch); iOS 15.2; Scale/3.00)",
        "Element/1.8.21 (iPad Pro (12.9-inch) (3rd generation); iOS 15.2; Scale/3.00)",
)
private val AN_EXPECTED_RESULT_LIST_FOR_IOS = listOf(
        DeviceExtendedInfo(DeviceType.MOBILE, "iPhone", "iOS 15.2", "Element", "1.8.21"),
        DeviceExtendedInfo(DeviceType.MOBILE, "iPhone XS Max", "iOS 15.2", "Element", "1.8.21"),
        DeviceExtendedInfo(DeviceType.MOBILE, "iPad Pro (11-inch)", "iOS 15.2", "Element", "1.8.21"),
        DeviceExtendedInfo(DeviceType.MOBILE, "iPad Pro (12.9-inch) (3rd generation)", "iOS 15.2",
                "Element", "1.8.21"),
)

private val A_USER_AGENT_LIST_FOR_DESKTOP = listOf(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) ElementNightly/2022091301 Chrome/104.0.5112.102" +
                " Electron/20.1.1 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) ElementNightly/2022091301 Chrome/104.0.5112.102 Electron/20.1.1 Safari/537.36",
)
private val AN_EXPECTED_RESULT_LIST_FOR_DESKTOP = listOf(
        DeviceExtendedInfo(DeviceType.DESKTOP, null, "Macintosh", "Electron", "20"),
        DeviceExtendedInfo(DeviceType.DESKTOP, null, "Windows NT 10.0", "Electron", "20"),
)

private val A_USER_AGENT_LIST_FOR_WEB = listOf(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.102 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.102 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:39.0) Gecko/20100101 Firefox/39.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/600.3.18 (KHTML, like Gecko) Version/8.0.3 Safari/600.3.18",
        "Mozilla/5.0 (Linux; Android 9; SM-G973U Build/PPR1.180610.011) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Mobile Safari/537.36",
        "Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12H321 Safari/600.1.4",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12H321 Safari/600.1.4",
        "Mozilla/5.0 (Windows NT 6.0; rv:40.0) Gecko/20100101 Firefox/40.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246",
        )
private val AN_EXPECTED_RESULT_LIST_FOR_WEB = listOf(
        DeviceExtendedInfo(DeviceType.WEB, null, "Macintosh", "Chrome", "104"),
        DeviceExtendedInfo(DeviceType.WEB, null, "Windows NT 10.0", "Chrome", "104"),
        DeviceExtendedInfo(DeviceType.WEB, null, "Macintosh", "Firefox", "39"),
        DeviceExtendedInfo(DeviceType.WEB, null, "Macintosh", "Safari", "8"),
        DeviceExtendedInfo(DeviceType.WEB, null, "Android 9", "Chrome", "69"),
        DeviceExtendedInfo(DeviceType.WEB, null, "iPad", "Safari", "8"),
        DeviceExtendedInfo(DeviceType.WEB, null, "iPhone", "Safari", "8"),
        DeviceExtendedInfo(DeviceType.WEB, null, "Windows NT 6.0", "Firefox", "40"),
        DeviceExtendedInfo(DeviceType.WEB, null, "Windows NT 10.0", "Edge", "12"),
)

private val AN_UNKNOWN_USER_AGENT_LIST = listOf(
        "AppleTV11,1/11.1",
        "Curl Client/1.0",
)
private val AN_UNKNOWN_USER_AGENT_EXPECTED_RESULT_LIST = listOf(
        DeviceExtendedInfo(DeviceType.UNKNOWN, null, null, null, null),
        DeviceExtendedInfo(DeviceType.UNKNOWN, null, null, null, null),
)

class ParseDeviceUserAgentUseCaseTest {

    private val parseDeviceUserAgentUseCase = ParseDeviceUserAgentUseCase()

    @Test
    fun `given an Android user agent then it should be parsed as expected`() {
        A_USER_AGENT_LIST_FOR_ANDROID.forEachIndexed { index, userAgent ->
            parseDeviceUserAgentUseCase.execute(userAgent) shouldBeEqualTo AN_EXPECTED_RESULT_LIST_FOR_ANDROID[index]
        }
    }

    @Test
    fun `given an iOS user agent then it should be parsed as expected`() {
        A_USER_AGENT_LIST_FOR_IOS.forEachIndexed { index, userAgent ->
            parseDeviceUserAgentUseCase.execute(userAgent) shouldBeEqualTo AN_EXPECTED_RESULT_LIST_FOR_IOS[index]
        }
    }

    @Test
    fun `given a Desktop user agent then it should be parsed as expected`() {
        A_USER_AGENT_LIST_FOR_DESKTOP.forEachIndexed { index, userAgent ->
            parseDeviceUserAgentUseCase.execute(userAgent) shouldBeEqualTo AN_EXPECTED_RESULT_LIST_FOR_DESKTOP[index]
        }
    }

    @Test
    fun `given a Web user agent then it should be parsed as expected`() {
        A_USER_AGENT_LIST_FOR_WEB.forEachIndexed { index, userAgent ->
            parseDeviceUserAgentUseCase.execute(userAgent) shouldBeEqualTo AN_EXPECTED_RESULT_LIST_FOR_WEB[index]
        }
    }

    @Test
    fun `given an unknown user agent then it should be parsed as expected`() {
        AN_UNKNOWN_USER_AGENT_LIST.forEachIndexed { index, userAgent ->
            parseDeviceUserAgentUseCase.execute(userAgent) shouldBeEqualTo AN_UNKNOWN_USER_AGENT_EXPECTED_RESULT_LIST[index]
        }
    }
}
