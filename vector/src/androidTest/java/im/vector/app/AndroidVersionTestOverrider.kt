/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import android.os.Build
import java.lang.reflect.Field

/**
 * Used to override [Build.VERSION.SDK_INT]. Ideally an interface should be used instead, but that approach forces us to either add suppress lint annotations
 * and potentially miss an API version issue or write a custom lint rule, which seems like an overkill.
 */
object AndroidVersionTestOverrider {

    private var initialValue: Int? = null

    fun override(newVersion: Int) {
        if (initialValue == null) {
            initialValue = Build.VERSION.SDK_INT
        }
        val field = Build.VERSION::class.java.getField("SDK_INT")
        setStaticField(field, newVersion)
    }

    fun restore() {
        initialValue?.let { override(it) }
    }

    private fun setStaticField(field: Field, value: Any) {
        field.isAccessible = true
        field.set(null, value)
    }
}
