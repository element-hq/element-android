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

package im.vector.app.features.widgets

import android.Manifest
import android.webkit.PermissionRequest
import im.vector.app.features.widgets.webview.WebviewPermissionUtils
import im.vector.app.test.fakes.FakeVectorPreferences
import org.amshove.kluent.shouldBeEqualTo
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class WebviewPermissionUtilsTest {

    private val prefs = FakeVectorPreferences()
    private val utils = WebviewPermissionUtils(prefs.instance)

    @Test
    fun filterPermissionsToBeGranted_selectedAndGrantedNothing() {
        val permissions = utils.filterPermissionsToBeGranted(
                selectedWebPermissions = listOf(),
                androidPermissionResult = mapOf()
        )
        permissions shouldBeEqualTo listOf()
    }

    @Test
    fun filterPermissionsToBeGranted_selectedNothingGrantedCamera() {
        val permissions = utils.filterPermissionsToBeGranted(
                selectedWebPermissions = listOf(),
                androidPermissionResult = mapOf(Manifest.permission.CAMERA to true)
        )
        permissions shouldBeEqualTo listOf()
    }

    @Test
    fun filterPermissionsToBeGranted_selectedAndPreviouslyGrantedCamera() {
        val permissions = utils.filterPermissionsToBeGranted(
                selectedWebPermissions = listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
                androidPermissionResult = mapOf()
        )
        permissions shouldBeEqualTo listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
    }

    @Test
    fun filterPermissionsToBeGranted_selectedAndGrantedCamera() {
        val permissions = utils.filterPermissionsToBeGranted(
                selectedWebPermissions = listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
                androidPermissionResult = mapOf(Manifest.permission.CAMERA to true)
        )
        permissions shouldBeEqualTo listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
    }

    @Test
    fun filterPermissionsToBeGranted_selectedAndDeniedCamera() {
        val permissions = utils.filterPermissionsToBeGranted(
                selectedWebPermissions = listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
                androidPermissionResult = mapOf(Manifest.permission.CAMERA to false)
        )
        permissions shouldBeEqualTo listOf()
    }

    @Test
    fun filterPermissionsToBeGranted_selectedProtectedMediaGrantedNothing() {
        val permissions = utils.filterPermissionsToBeGranted(
                selectedWebPermissions = listOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID),
                androidPermissionResult = mapOf(Manifest.permission.CAMERA to false)
        )
        permissions shouldBeEqualTo listOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)
    }
}
