/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import im.vector.app.R
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.resources.isEnglishSpeaking
import im.vector.app.core.utils.colorTerminatingFullStop
import im.vector.app.features.themes.ThemeProvider
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class SplashCarouselStateFactory @Inject constructor(
        private val context: Context,
        private val stringProvider: StringProvider,
        private val localeProvider: LocaleProvider,
        private val themeProvider: ThemeProvider,
) {

    fun create(): SplashCarouselState {
        val lightTheme = themeProvider.isLightTheme()
        fun background(@DrawableRes lightDrawable: Int) = if (lightTheme) lightDrawable else im.vector.lib.ui.styles.R.drawable.bg_color_background
        fun hero(@DrawableRes lightDrawable: Int, @DrawableRes darkDrawable: Int) = if (lightTheme) lightDrawable else darkDrawable
        return SplashCarouselState(
                listOf(
                        SplashCarouselState.Item(
                                CommonStrings.ftue_auth_carousel_secure_title.colorTerminatingFullStop(com.google.android.material.R.attr.colorAccent),
                                CommonStrings.ftue_auth_carousel_secure_body,
                                hero(R.drawable.ic_splash_conversations, R.drawable.ic_splash_conversations_dark),
                                background(im.vector.lib.ui.styles.R.drawable.bg_carousel_page_1)
                        ),
                        SplashCarouselState.Item(
                                CommonStrings.ftue_auth_carousel_control_title.colorTerminatingFullStop(com.google.android.material.R.attr.colorAccent),
                                CommonStrings.ftue_auth_carousel_control_body,
                                hero(R.drawable.ic_splash_control, R.drawable.ic_splash_control_dark),
                                background(im.vector.lib.ui.styles.R.drawable.bg_carousel_page_2)
                        ),
                        SplashCarouselState.Item(
                                CommonStrings.ftue_auth_carousel_encrypted_title.colorTerminatingFullStop(com.google.android.material.R.attr.colorAccent),
                                CommonStrings.ftue_auth_carousel_encrypted_body,
                                hero(R.drawable.ic_splash_secure, R.drawable.ic_splash_secure_dark),
                                background(im.vector.lib.ui.styles.R.drawable.bg_carousel_page_3)
                        ),
                        SplashCarouselState.Item(
                                collaborationTitle().colorTerminatingFullStop(com.google.android.material.R.attr.colorAccent),
                                CommonStrings.ftue_auth_carousel_workplace_body,
                                hero(R.drawable.ic_splash_collaboration, R.drawable.ic_splash_collaboration_dark),
                                background(im.vector.lib.ui.styles.R.drawable.bg_carousel_page_4)
                        )
                )
        )
    }

    private fun collaborationTitle(): Int {
        return when {
            localeProvider.isEnglishSpeaking() -> CommonStrings.cut_the_slack_from_teams
            else -> CommonStrings.ftue_auth_carousel_workplace_title
        }
    }

    private fun Int.colorTerminatingFullStop(@AttrRes color: Int): EpoxyCharSequence {
        return stringProvider.getString(this)
                .colorTerminatingFullStop(ThemeUtils.getColor(context, color))
                .toEpoxyCharSequence()
    }
}
