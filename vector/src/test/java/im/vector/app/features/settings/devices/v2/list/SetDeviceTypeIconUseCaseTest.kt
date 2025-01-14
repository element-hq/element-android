/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
