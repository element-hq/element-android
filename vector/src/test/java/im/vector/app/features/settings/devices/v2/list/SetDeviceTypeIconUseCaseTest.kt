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

package im.vector.app.features.settings.devices.v2.list

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.app.R
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.lib.strings.CommonStrings
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Test

private const val A_DESCRIPTION = "description"

class SetDeviceTypeIconUseCaseTest {

    private val fakeStringProvider = FakeStringProvider()

    private val setDeviceTypeIconUseCase = SetDeviceTypeIconUseCase()

    @Test
    fun `given a device type when execute then correct icon and description is set to the ImageView`() {
        testType(
                deviceType = DeviceType.UNKNOWN,
                drawableResId = R.drawable.ic_device_type_unknown,
                descriptionResId = CommonStrings.a11y_device_manager_device_type_unknown
        )

        testType(
                deviceType = DeviceType.MOBILE,
                drawableResId = R.drawable.ic_device_type_mobile,
                descriptionResId = CommonStrings.a11y_device_manager_device_type_mobile
        )

        testType(
                deviceType = DeviceType.WEB,
                drawableResId = R.drawable.ic_device_type_web,
                descriptionResId = CommonStrings.a11y_device_manager_device_type_web
        )

        testType(
                deviceType = DeviceType.DESKTOP,
                drawableResId = R.drawable.ic_device_type_desktop,
                descriptionResId = CommonStrings.a11y_device_manager_device_type_desktop
        )
    }

    private fun testType(deviceType: DeviceType, @DrawableRes drawableResId: Int, @StringRes descriptionResId: Int) {
        // Given
        val imageView = mockk<ImageView>(relaxUnitFun = true)
        fakeStringProvider.given(descriptionResId, A_DESCRIPTION)

        // When
        setDeviceTypeIconUseCase.execute(deviceType, imageView, fakeStringProvider.instance)

        // Then
        verifyAll {
            imageView.setImageResource(drawableResId)
            imageView.contentDescription = A_DESCRIPTION
        }
    }
}
