/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.test.fakes

import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class FakeVectorPreferences {

    val instance = mockk<VectorPreferences>(relaxUnitFun = true)

    fun givenUseCompleteNotificationFormat(value: Boolean) {
        every { instance.useCompleteNotificationFormat() } returns value
    }

    fun givenSpaceBackstack(value: List<String?>) {
        every { instance.getSpaceBackstack() } returns value
    }

    fun verifySetSpaceBackstack(value: List<String?>, inverse: Boolean = false) {
        verify(inverse = inverse) { instance.setSpaceBackstack(value) }
    }

    fun givenIsClientInfoRecordingEnabled(isEnabled: Boolean) {
        every { instance.isClientInfoRecordingEnabled() } returns isEnabled
    }

    fun givenTextFormatting(isEnabled: Boolean) =
            every { instance.isTextFormattingEnabled() } returns isEnabled

    fun givenSetNotificationEnabledForDevice() {
        justRun { instance.setNotificationEnabledForDevice(any()) }
    }

    fun verifySetNotificationEnabledForDevice(enabled: Boolean, inverse: Boolean = false) {
        verify(inverse = inverse) { instance.setNotificationEnabledForDevice(enabled) }
    }

    fun givenSessionManagerShowIpAddress(showIpAddress: Boolean) {
        every { instance.showIpAddressInSessionManagerScreens() } returns showIpAddress
    }

    fun givenUnverifiedSessionsAlertLastShownMillis(lastShownMillis: Long) {
        every { instance.getUnverifiedSessionsAlertLastShownMillis(any()) } returns lastShownMillis
    }

    fun givenSetFdroidSyncBackgroundMode(mode: BackgroundSyncMode) {
        justRun { instance.setFdroidSyncBackgroundMode(mode) }
    }

    fun verifySetFdroidSyncBackgroundMode(mode: BackgroundSyncMode) {
        verify { instance.setFdroidSyncBackgroundMode(mode) }
    }

    fun givenAreNotificationsEnabledForDevice(notificationsEnabled: Boolean) {
        every { instance.areNotificationEnabledForDevice() } returns notificationsEnabled
    }

    fun givenIsBackgroundSyncEnabled(isEnabled: Boolean) {
        every { instance.isBackgroundSyncEnabled() } returns isEnabled
    }

    fun givenShowIpAddressInSessionManagerScreens(show: Boolean) {
        every { instance.showIpAddressInSessionManagerScreens() } returns show
    }

    fun verifySetIpAddressVisibilityInDeviceManagerScreens(isVisible: Boolean) {
        verify { instance.setIpAddressVisibilityInDeviceManagerScreens(isVisible) }
    }

    fun givenIsVoiceBroadcastEnabled(isEnabled: Boolean) {
        every { instance.isVoiceBroadcastEnabled() } returns isEnabled
    }
}
