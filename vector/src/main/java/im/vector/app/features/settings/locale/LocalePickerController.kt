/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.locale

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.epoxy.profiles.profileSectionItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.safeCapitalize
import im.vector.app.features.settings.VectorLocale
import im.vector.app.features.settings.VectorPreferences
import java.util.Locale
import javax.inject.Inject

class LocalePickerController @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<LocalePickerViewState>() {

    var listener: Listener? = null

    override fun buildModels(data: LocalePickerViewState?) {
        val list = data?.locales ?: return
        val host = this

        profileSectionItem {
            id("currentTitle")
            title(host.stringProvider.getString(R.string.choose_locale_current_locale_title))
        }
        localeItem {
            id(data.currentLocale.toString())
            title(VectorLocale.localeToLocalisedString(data.currentLocale).safeCapitalize(data.currentLocale))
            if (host.vectorPreferences.developerMode()) {
                subtitle(VectorLocale.localeToLocalisedStringInfo(data.currentLocale))
            }
            clickListener { host.listener?.onUseCurrentClicked() }
        }
        profileSectionItem {
            id("otherTitle")
            title(host.stringProvider.getString(R.string.choose_locale_other_locales_title))
        }
        when (list) {
            Uninitialized,
            is Loading -> {
                loadingItem {
                    id("loading")
                    loadingText(host.stringProvider.getString(R.string.choose_locale_loading_locales))
                }
            }
            is Success ->
                if (list().isEmpty()) {
                    noResultItem {
                        id("noResult")
                        text(host.stringProvider.getString(R.string.no_result_placeholder))
                    }
                } else {
                    list()
                            .filter { it.toString() != data.currentLocale.toString() }
                            .forEach { locale ->
                                localeItem {
                                    id(locale.toString())
                                    title(VectorLocale.localeToLocalisedString(locale).safeCapitalize(locale))
                                    if (host.vectorPreferences.developerMode()) {
                                        subtitle(VectorLocale.localeToLocalisedStringInfo(locale))
                                    }
                                    clickListener { host.listener?.onLocaleClicked(locale) }
                                }
                            }
                }
            is Fail ->
                errorWithRetryItem {
                    id("error")
                    text(host.errorFormatter.toHumanReadable(list.error))
                }
        }
    }

    interface Listener {
        fun onUseCurrentClicked()
        fun onLocaleClicked(locale: Locale)
    }
}
