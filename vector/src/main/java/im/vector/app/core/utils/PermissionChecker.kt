/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

interface PermissionChecker {

    @InstallIn(SingletonComponent::class)
    @dagger.Module
    interface Module {
        @Binds
        fun bindPermissionChecker(permissionChecker: AndroidPermissionChecker): PermissionChecker
    }

    fun checkPermission(vararg permissions: String): Boolean
}

class AndroidPermissionChecker @Inject constructor(
        private val applicationContext: Context,
) : PermissionChecker {
    override fun checkPermission(vararg permissions: String): Boolean {
        return permissions.any { permission ->
            ActivityCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
