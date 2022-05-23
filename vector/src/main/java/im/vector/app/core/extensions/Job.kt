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
