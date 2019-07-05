/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.home

import androidx.annotation.ColorRes
import im.vector.riotx.R
import kotlin.math.abs


@ColorRes
fun getColorFromUserId(userId: String?): Int {
    if (userId.isNullOrBlank()) {
        return R.color.riotx_username_1
    }

    var hash = 0
    var i = 0
    var chr: Char

    while (i < userId.length) {
        chr = userId[i]
        hash = (hash shl 5) - hash + chr.toInt()
        i++
    }

    return when (abs(hash) % 8 + 1) {
        1    -> R.color.riotx_username_1
        2    -> R.color.riotx_username_2
        3    -> R.color.riotx_username_3
        4    -> R.color.riotx_username_4
        5    -> R.color.riotx_username_5
        6    -> R.color.riotx_username_6
        7    -> R.color.riotx_username_7
        else -> R.color.riotx_username_8
    }
}