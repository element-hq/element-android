/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import kotlinx.coroutines.Job
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate for automatically cancelling the current job when setting a new value.
 */
fun cancelCurrentOnSet(): ReadWriteProperty<Any?, Job?> = object : ReadWriteProperty<Any?, Job?> {
    private var currentJob: Job? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>): Job? = currentJob
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Job?) {
        currentJob?.cancel()
        currentJob = value
    }
}
