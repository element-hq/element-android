/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

data class SplashCarouselState(
        val items: List<Item>
) {
    data class Item(
            val title: EpoxyCharSequence,
            @StringRes val body: Int,
            @DrawableRes val image: Int,
            @DrawableRes val pageBackground: Int
    )
}
