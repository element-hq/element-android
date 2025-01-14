/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import android.content.res.Resources
import androidx.annotation.ArrayRes
import javax.inject.Inject

class StringArrayProvider @Inject constructor(private val resources: Resources) {

    /**
     * Returns a localized string array from the application's package's
     * default string array table.
     *
     * @param resId Resource id for the string array
     * @return The string array associated with the resource, stripped of styled
     * text information.
     */
    fun getStringArray(@ArrayRes resId: Int): Array<String> {
        return resources.getStringArray(resId)
    }
}
