/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

fun <T> weak(value: T) = WeakReferenceDelegate(value)

class WeakReferenceDelegate<T>(value: T) {

    private var weakReference: WeakReference<T> = WeakReference(value)

    operator fun getValue(thisRef: Any, property: KProperty<*>): T? = weakReference.get()
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        weakReference = WeakReference(value)
    }
}
