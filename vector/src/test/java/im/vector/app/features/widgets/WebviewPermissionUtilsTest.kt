/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
