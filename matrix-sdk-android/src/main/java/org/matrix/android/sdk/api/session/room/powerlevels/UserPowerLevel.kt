/*
 * Copyright 2025 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.matrix.android.sdk.api.session.room.powerlevels

sealed interface UserPowerLevel : Comparable<UserPowerLevel> {
    data object Infinite : UserPowerLevel

    @JvmInline
    value class Value(val value: Int) : UserPowerLevel

    override fun compareTo(other: UserPowerLevel): Int {
        return when (this) {
            Infinite -> when (other) {
                Infinite -> 0
                is Value -> 1
            }
            is Value -> when (other) {
                Infinite -> -1
                is Value -> value.compareTo(other.value)
            }
        }
    }

    companion object {
        val User = Value(0)
        val Moderator = Value(50)
        val Admin = Value(100)
        val SuperAdmin = Value(150)
    }
}
