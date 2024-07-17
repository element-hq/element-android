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

package im.vector.app.features.settings.font

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.fontScaleItem
import im.vector.app.core.epoxy.fontScaleSectionItem
import im.vector.app.core.epoxy.fontScaleUseSystemSettingsItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.FontScaleValue
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class FontScaleSettingController @Inject constructor(
        val stringProvider: StringProvider
) : TypedEpoxyController<FontScaleSettingViewState>() {

    var callback: Callback? = null

    override fun buildModels(data: FontScaleSettingViewState?) {
        data?.let {
            buildAutomaticallySection(data.useSystemSettings)
            buildFontScaleItems(data.availableScaleOptions, data.persistedSettingIndex, data.useSystemSettings)
        }
    }

    private fun buildAutomaticallySection(useSystemSettings: Boolean) {
        val host = this
        fontScaleSectionItem {
            id("section_automatically")
            sectionName(host.stringProvider.getString(CommonStrings.font_size_section_auto))
        }

        fontScaleUseSystemSettingsItem {
            id("use_system_settings")
            useSystemSettings(useSystemSettings)
            checkChangeListener { _, isChecked ->
                host.callback?.onUseSystemSettingChanged(useSystemSettings = isChecked)
            }
        }
    }

    private fun buildFontScaleItems(scales: List<FontScaleValue>, persistedSettingIndex: Int, useSystemSettings: Boolean) {
        val host = this
        fontScaleSectionItem {
            id("section_manually")
            sectionName(host.stringProvider.getString(CommonStrings.font_size_section_manually))
        }

        scales.forEachIndexed { index, scaleItem ->
            fontScaleItem {
                id(scaleItem.index)
                fontScale(scaleItem)
                selected(index == persistedSettingIndex)
                enabled(!useSystemSettings)
                checkChangeListener { _, isChecked ->
                    if (isChecked) {
                        host.callback?.oFontScaleSelected(fonScale = scaleItem)
                    }
                }
            }
        }
    }

    interface Callback {
        fun onUseSystemSettingChanged(useSystemSettings: Boolean)
        fun oFontScaleSelected(fonScale: FontScaleValue)
    }
}
