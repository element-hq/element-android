/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object DefaultSharedPreferences {

    @Volatile private var INSTANCE: SharedPreferences? = null

    fun getInstance(context: Context): SharedPreferences =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceManager.getDefaultSharedPreferences(context.applicationContext).also { INSTANCE = it }
            }
}
