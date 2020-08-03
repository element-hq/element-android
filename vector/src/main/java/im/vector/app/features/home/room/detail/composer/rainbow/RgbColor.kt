/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer.rainbow

data class RgbColor(
        val r: Int,
        val g: Int,
        val b: Int
)

fun RgbColor.toDashColor(): String {
    return listOf(r, g, b)
            .joinToString(separator = "", prefix = "#") {
                it.toString(16).padStart(2, '0')
            }
}
