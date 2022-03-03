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

package im.vector.app.features.onboarding.ftueauth

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import im.vector.app.R
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.isEnglishSpeaking
import im.vector.app.features.themes.ThemeProvider
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import javax.inject.Inject

class SplashCarouselStateFactory @Inject constructor(
        private val context: Context,
        private val stringProvider: StringProvider,
        private val localeProvider: LocaleProvider,
        private val themeProvider: ThemeProvider,
) {

    fun create(): SplashCarouselState {
        val lightTheme = themeProvider.isLightTheme()
        fun background(@DrawableRes lightDrawable: Int) = if (lightTheme) lightDrawable else R.drawable.bg_carousel_page_dark
        fun hero(@DrawableRes lightDrawable: Int, @DrawableRes darkDrawable: Int) = if (lightTheme) lightDrawable else darkDrawable
        return SplashCarouselState(listOf(
                SplashCarouselState.Item(
                        R.string.ftue_auth_carousel_secure_title.colorTerminatingFullStop(R.attr.colorAccent),
                        R.string.ftue_auth_carousel_secure_body,
                        hero(R.drawable.ic_splash_conversations, R.drawable.ic_splash_conversations_dark),
                        background(R.drawable.bg_carousel_page_1)
                ),
                SplashCarouselState.Item(
                        R.string.ftue_auth_carousel_control_title.colorTerminatingFullStop(R.attr.colorAccent),
                        R.string.ftue_auth_carousel_control_body,
                        hero(R.drawable.ic_splash_control, R.drawable.ic_splash_control_dark),
                        background(R.drawable.bg_carousel_page_2)
                ),
                SplashCarouselState.Item(
                        R.string.ftue_auth_carousel_encrypted_title.colorTerminatingFullStop(R.attr.colorAccent),
                        R.string.ftue_auth_carousel_encrypted_body,
                        hero(R.drawable.ic_splash_secure, R.drawable.ic_splash_secure_dark),
                        background(R.drawable.bg_carousel_page_3)
                ),
                SplashCarouselState.Item(
                        collaborationTitle().colorTerminatingFullStop(R.attr.colorAccent),
                        R.string.ftue_auth_carousel_workplace_body,
                        hero(R.drawable.ic_splash_collaboration, R.drawable.ic_splash_collaboration_dark),
                        background(R.drawable.bg_carousel_page_4)
                )
        ))
    }

    private fun collaborationTitle(): Int {
        return when {
            localeProvider.isEnglishSpeaking() -> R.string.cut_the_slack_from_teams
            else                               -> R.string.ftue_auth_carousel_workplace_title
        }
    }

    private fun Int.colorTerminatingFullStop(@AttrRes color: Int): EpoxyCharSequence {
        val string = stringProvider.getString(this)
        val fullStop = "."
        val charSequence = if (string.endsWith(fullStop)) {
            span {
                +string.removeSuffix(fullStop)
                span(fullStop) {
                    textColor = ThemeUtils.getColor(context, color)
                }
            }
        } else {
            string
        }
        return charSequence.toEpoxyCharSequence()
    }
}
