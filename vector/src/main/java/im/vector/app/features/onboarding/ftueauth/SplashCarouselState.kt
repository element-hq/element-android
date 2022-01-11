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
import im.vector.app.R

data class SplashCarouselState(
        val items: List<Item> = listOf(
                Item(
                        R.string.ftue_auth_carousel_1_title,
                        R.string.ftue_auth_carousel_1_body,
                        R.drawable.onboarding_carousel_conversations,
                        R.drawable.bg_carousel_page_1
                ),
                Item(
                        R.string.ftue_auth_carousel_2_title,
                        R.string.ftue_auth_carousel_2_body,
                        R.drawable.onboarding_carousel_ems,
                        R.drawable.bg_carousel_page_2
                ),
                Item(
                        R.string.ftue_auth_carousel_3_title,
                        R.string.ftue_auth_carousel_3_body,
                        R.drawable.onboarding_carousel_connect,
                        R.drawable.bg_carousel_page_3
                ),
                Item(
                        R.string.ftue_auth_carousel_4_title,
                        R.string.ftue_auth_carousel_4_body,
                        R.drawable.onboarding_carousel_universal,
                        R.drawable.bg_carousel_page_4
                )
        )
) {
    data class Item(
            @StringRes val title: Int,
            @StringRes val body: Int,
            @DrawableRes val image: Int,
            @DrawableRes val pageBackground: Int
    )
}
