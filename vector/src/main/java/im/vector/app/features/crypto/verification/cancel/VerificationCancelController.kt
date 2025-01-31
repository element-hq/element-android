/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.cancel

import androidx.core.text.toSpannable
import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewState
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.displayname.getBestName
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import javax.inject.Inject

class VerificationCancelController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationBottomSheetViewState? = null

    fun update(viewState: VerificationBottomSheetViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val state = viewState ?: return
        val host = this
        if (state.isMe) {
            if (state.currentDeviceCanCrossSign) {
                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(host.stringProvider.getString(R.string.verify_cancel_self_verification_from_trusted).toEpoxyCharSequence())
                }
            } else {
                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(host.stringProvider.getString(R.string.verify_cancel_self_verification_from_untrusted).toEpoxyCharSequence())
                }
            }
        } else {
            val otherUserID = state.otherUserId
            val otherDisplayName = state.otherUserMxItem.getBestName()
            bottomSheetVerificationNoticeItem {
                id("notice")
                notice(
                        EpoxyCharSequence(
                                host.stringProvider.getString(R.string.verify_cancel_other, otherDisplayName, otherUserID)
                                        .toSpannable()
                                        .colorizeMatchingText(otherUserID, host.colorProvider.getColorFromAttribute(R.attr.vctr_notice_text_color))
                        )
                )
            }
        }

        bottomSheetDividerItem {
            id("sep0")
        }

        bottomSheetVerificationActionItem {
            id("cancel")
            title(host.stringProvider.getString(R.string.action_skip))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
            listener { host.listener?.onTapCancel() }
        }

        bottomSheetDividerItem {
            id("sep1")
        }

        bottomSheetVerificationActionItem {
            id("continue")
            title(host.stringProvider.getString(R.string._continue))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
            listener { host.listener?.onTapContinue() }
        }
    }

    interface Listener {
        fun onTapCancel()
        fun onTapContinue()
    }
}
