/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.settings.locale

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.noResultItem
import im.vector.riotx.core.epoxy.profiles.profileSectionItem
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.settings.VectorLocale
import im.vector.riotx.features.settings.VectorPreferences
import java.util.Locale
import javax.inject.Inject

class LocalePickerController @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val stringProvider: StringProvider
) : TypedEpoxyController<LocalePickerViewState>() {

    var listener: Listener? = null

    @ExperimentalStdlibApi
    override fun buildModels(data: LocalePickerViewState?) {
        val list = data?.locales ?: return

        if (list.isEmpty()) {
            noResultItem {
                id("noResult")
                text(stringProvider.getString(R.string.no_result_placeholder))
            }
        } else {
            profileSectionItem {
                id("currentTitle")
                title(stringProvider.getString(R.string.choose_locale_current_locale_title))
            }
            localeItem {
                id(data.currentLocale.toString())
                title(VectorLocale.localeToLocalisedString(data.currentLocale).capitalize(data.currentLocale))
                if (vectorPreferences.developerMode()) {
                    subtitle(VectorLocale.localeToLocalisedStringInfo(data.currentLocale))
                }
                clickListener { listener?.onUseCurrentClicked() }
            }
            profileSectionItem {
                id("otherTitle")
                title(stringProvider.getString(R.string.choose_locale_other_locales_title))
            }
            list
                    .filter { it != data.currentLocale }
                    .forEach {
                        localeItem {
                            id(it.toString())
                            title(VectorLocale.localeToLocalisedString(it).capitalize(it))
                            if (vectorPreferences.developerMode()) {
                                subtitle(VectorLocale.localeToLocalisedStringInfo(it))
                            }
                            clickListener { listener?.onLocaleClicked(it) }
                        }
                    }
        }
    }

    interface Listener {
        fun onUseCurrentClicked()
        fun onLocaleClicked(locale: Locale)
    }
}
