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

package im.vector.app.core.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import androidx.core.content.ContextCompat.checkSelfPermission
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test

class CheckWebViewPermissionsUseCaseTest {

    private val checkWebViewPermissionsUseCase = CheckWebViewPermissionsUseCase()

    private val activity = mockk<Activity>().apply {
        every { applicationContext } returns mockk()
    }

    @Before
    fun setup() {
        mockkStatic("androidx.core.content.ContextCompat")
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.core.content.ContextCompat")
    }

    @Test
    fun `given an audio permission is granted when the web client requests audio permission then use case returns true`() {
        val permissionRequest = givenAPermissionRequest(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        every { checkSelfPermission(activity.applicationContext, any()) } returns PackageManager.PERMISSION_GRANTED

        checkWebViewPermissionsUseCase.execute(activity, permissionRequest) shouldBe true
        verifyPermissionsChecked(activity.applicationContext, PERMISSIONS_FOR_AUDIO_IP_CALL)
    }

    @Test
    fun `given a camera permission is granted when the web client requests video permission then use case returns true`() {
        val permissionRequest = givenAPermissionRequest(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        every { checkSelfPermission(activity.applicationContext, any()) } returns PackageManager.PERMISSION_GRANTED

        checkWebViewPermissionsUseCase.execute(activity, permissionRequest) shouldBe true
        verifyPermissionsChecked(activity.applicationContext, PERMISSIONS_FOR_VIDEO_IP_CALL)
    }

    @Test
    fun `given an audio and camera permissions are granted when the web client requests audio and video permissions then use case returns true`() {
        val permissionRequest = givenAPermissionRequest(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE, PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        every { checkSelfPermission(activity.applicationContext, any()) } returns PackageManager.PERMISSION_GRANTED

        checkWebViewPermissionsUseCase.execute(activity, permissionRequest) shouldBe true
        verifyPermissionsChecked(activity.applicationContext, PERMISSIONS_FOR_AUDIO_IP_CALL + PERMISSIONS_FOR_VIDEO_IP_CALL)
    }

    @Test
    fun `given an audio permission is granted but camera isn't when the web client requests audio and video permissions then use case returns false`() {
        val permissionRequest = givenAPermissionRequest(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE, PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        PERMISSIONS_FOR_AUDIO_IP_CALL.forEach {
            every { checkSelfPermission(activity.applicationContext, it) } returns PackageManager.PERMISSION_GRANTED
        }
        PERMISSIONS_FOR_VIDEO_IP_CALL.forEach {
            every { checkSelfPermission(activity.applicationContext, it) } returns PackageManager.PERMISSION_DENIED
        }

        checkWebViewPermissionsUseCase.execute(activity, permissionRequest) shouldBe false
        verifyPermissionsChecked(activity.applicationContext, PERMISSIONS_FOR_AUDIO_IP_CALL + PERMISSIONS_FOR_VIDEO_IP_CALL.first())
    }

    @Test
    fun `given an audio and camera permissions are granted when the web client requests another permission then use case returns false`() {
        val permissionRequest = givenAPermissionRequest(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE, PermissionRequest.RESOURCE_MIDI_SYSEX))
        every { checkSelfPermission(activity.applicationContext, any()) } returns PackageManager.PERMISSION_GRANTED

        checkWebViewPermissionsUseCase.execute(activity, permissionRequest) shouldBe false
        verifyPermissionsChecked(activity.applicationContext, PERMISSIONS_FOR_AUDIO_IP_CALL)
    }

    private fun verifyPermissionsChecked(context: Context, permissions: List<String>) {
        permissions.forEach {
            verify { checkSelfPermission(context, it) }
        }
    }

    private fun givenAPermissionRequest(resources: Array<String>): PermissionRequest {
        return object : PermissionRequest() {
            override fun getOrigin(): Uri {
                return mockk()
            }

            override fun getResources(): Array<String> {
                return resources
            }

            override fun grant(resources: Array<out String>?) {
            }

            override fun deny() {
            }
        }
    }
}
