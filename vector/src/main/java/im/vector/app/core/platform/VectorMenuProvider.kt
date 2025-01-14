/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes

/**
 * Let your Activity of Fragment implement this interface if they provide a Menu.
 */
interface VectorMenuProvider {
    @MenuRes
    fun getMenuRes(): Int

    // No op by default
    fun handlePostCreateMenu(menu: Menu) {}

    // No op by default
    fun handlePrepareMenu(menu: Menu) {}

    fun handleMenuItemSelected(item: MenuItem): Boolean
}
