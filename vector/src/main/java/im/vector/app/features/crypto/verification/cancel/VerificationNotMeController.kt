/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.cancel

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewState
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
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
        val host = this
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(host.eventHtmlRenderer.render(host.stringProvider.getString(R.string.verify_not_me_self_verification)).toEpoxyCharSequence())
        }

        bottomSheetDividerItem {
            id("sep0")
        }

        bottomSheetVerificationActionItem {
            id("skip")
            title(host.stringProvider.getString(R.string.action_skip))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
            listener { host.listener?.onTapSkip() }
        }

        bottomSheetDividerItem {
            id("sep1")
        }

        bottomSheetVerificationActionItem {
            id("settings")
            title(host.stringProvider.getString(R.string.settings))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
            listener { host.listener?.onTapSettings() }
        }
    }

    interface Listener {
        fun onTapSkip()
        fun onTapSettings()
    }
}
