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
