/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
