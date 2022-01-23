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

package im.vector.lib.core.utils.epoxy.charsequence

/**
 * Wrapper for a CharSequence, which support mutation of the CharSequence, which can happen during rendering
 */
class EpoxyCharSequence(val charSequence: CharSequence) {
    private val hash = charSequence.toString().hashCode()

    override fun hashCode() = hash
    override fun equals(other: Any?) = other is EpoxyCharSequence && other.hash == hash
}
