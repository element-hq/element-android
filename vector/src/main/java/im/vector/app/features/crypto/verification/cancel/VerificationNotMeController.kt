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

package im.vector.app.features.crypto.verification.cancel

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewState
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.html.EventHtmlRenderer
import javax.inject.Inject

class VerificationNotMeController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val eventHtmlRenderer: EventHtmlRenderer
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationBottomSheetViewState? = null

    fun update(viewState: VerificationBottomSheetViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(eventHtmlRenderer.render(stringProvider.getString(R.string.verify_not_me_self_verification)))
        }

        dividerItem {
            id("sep0")
        }

        bottomSheetVerificationActionItem {
            id("skip")
            title(stringProvider.getString(R.string.skip))
            titleColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
            listener { listener?.onTapSkip() }
        }

        dividerItem {
            id("sep1")
        }

        bottomSheetVerificationActionItem {
            id("settings")
            title(stringProvider.getString(R.string.settings))
            titleColor(colorProvider.getColor(R.color.riotx_positive_accent))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(colorProvider.getColor(R.color.riotx_positive_accent))
            listener { listener?.onTapSettings() }
        }
    }

    interface Listener {
        fun onTapSkip()
        fun onTapSettings()
    }
}
