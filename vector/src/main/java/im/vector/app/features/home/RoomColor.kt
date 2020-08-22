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

package im.vector.app.features.home

import androidx.annotation.ColorRes
import im.vector.app.R

@ColorRes
fun getColorFromRoomId(roomId: String?): Int {
    return when ((roomId?.toList()?.sumBy { it.toInt() } ?: 0) % 3) {
        1    -> R.color.riotx_avatar_fill_2
        2    -> R.color.riotx_avatar_fill_3
        else -> R.color.riotx_avatar_fill_1
    }
}
