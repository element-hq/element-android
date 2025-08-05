/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.utils.PermissionChecker

class FakePermissionChecker(val permissionResult: Boolean = true) : PermissionChecker {
    override fun checkPermission(vararg permissions: String): Boolean {
        return permissionResult
    }
}
