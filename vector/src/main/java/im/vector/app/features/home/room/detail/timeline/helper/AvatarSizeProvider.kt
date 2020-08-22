/*
 * Copyright 2019 New Vector Ltd
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
 */

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.utils.DimensionConverter
import javax.inject.Inject

class AvatarSizeProvider @Inject constructor(private val dimensionConverter: DimensionConverter) {

    private val avatarStyle = AvatarStyle.SMALL

    val leftGuideline: Int by lazy {
        dimensionConverter.dpToPx(avatarStyle.avatarSizeDP + 8)
    }

    val avatarSize: Int by lazy {
        dimensionConverter.dpToPx(avatarStyle.avatarSizeDP)
    }

    companion object {

        enum class AvatarStyle(val avatarSizeDP: Int) {
            BIG(50),
            MEDIUM(40),
            SMALL(30),
            NONE(0)
        }
    }
}
