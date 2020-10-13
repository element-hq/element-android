/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.util

data class Optional<T : Any> constructor(private val value: T?) {

    fun get(): T {
        return value!!
    }

    fun getOrNull(): T? {
        return value
    }

    fun <U : Any> map(fn: (T) -> U?): Optional<U> {
        return if (value == null) {
            from(null)
        } else {
            from(fn(value))
        }
    }

    fun getOrElse(fn: () -> T): T {
        return value ?: fn()
    }

    fun hasValue(): Boolean {
        return value != null
    }

    companion object {
        fun <T : Any> from(value: T?): Optional<T> {
            return Optional(value)
        }

        fun <T : Any> empty(): Optional<T> {
            return Optional(null)
        }
    }
}

fun <T : Any> T?.toOptional() = Optional(this)
