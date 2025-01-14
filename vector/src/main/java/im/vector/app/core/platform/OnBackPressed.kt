/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

interface OnBackPressed {

    /**
     * Returns true, if the on back pressed event has been handled by this Fragment.
     * Otherwise return false
     * @param toolbarButton true if this is the back button from the toolbar
     */
    fun onBackPressed(toolbarButton: Boolean): Boolean
}
