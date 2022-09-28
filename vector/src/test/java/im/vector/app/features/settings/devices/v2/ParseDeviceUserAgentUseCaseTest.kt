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
        // Legacy User Agent Implementation
        "Element/1.0.0 (Linux; U; Android 6.0.1; SM-A510F Build/MMB29; Flavour GPlay; MatrixAndroidSdk2 1.0)",
        "Element/1.0.0 (Linux; Android 7.0; SM-G610M Build/NRD90M; Flavour GPlay; MatrixAndroidSdk2 1.0)",
)
private val AN_EXPECTED_RESULT_LIST_FOR_ANDROID = listOf(
        DeviceUserAgent(DeviceType.MOBILE, "Xiaomi Mi 9T", "Android 11", "Element dbg", "1.5.0-dev"),
        DeviceUserAgent(DeviceType.MOBILE, "Samsung SM-G960F", "Android 6.0.1", "Element", "1.5.0"),
        DeviceUserAgent(DeviceType.MOBILE, "Google Nexus 5", "Android 7.0", "Element", "1.5.0"),
        DeviceUserAgent(DeviceType.MOBILE, "SM-A510F Build/MMB29", "Android 6.0.1", "Element", "1.0.0"),
        DeviceUserAgent(DeviceType.MOBILE, "SM-G610M Build/NRD90M", "Android 7.0", "Element", "1.0.0"),
)

private val A_USER_AGENT_LIST_FOR_IOS = listOf(
        "Element/1.8.21 (iPhone; iOS 15.2; Scale/3.00)",
        "Element/1.8.21 (iPhone XS Max; iOS 15.2; Scale/3.00)",
)
private val AN_EXPECTED_RESULT_LIST_FOR_IOS = listOf(
        DeviceUserAgent(DeviceType.MOBILE, "iPhone", "iOS 15.2", "Element", "1.8.21"),
        DeviceUserAgent(DeviceType.MOBILE, "iPhone XS Max", "iOS 15.2", "Element", "1.8.21"),
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
}
