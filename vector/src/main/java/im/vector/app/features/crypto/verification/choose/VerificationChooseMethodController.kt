/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.choose

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.buttonPositiveDestructiveButtonBarItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationQrCodeItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import javax.inject.Inject

class VerificationChooseMethodController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationChooseMethodViewState? = null

    fun update(viewState: VerificationChooseMethodViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val state = viewState ?: return
        val host = this

        if (state.otherCanScanQrCode || state.otherCanShowQrCode) {
            val scanCodeInstructions: String
            val scanOtherCodeTitle: String
            val compareEmojiSubtitle: String
            if (state.isMe) {
                scanCodeInstructions = stringProvider.getString(R.string.verification_scan_self_notice)
                scanOtherCodeTitle = stringProvider.getString(R.string.verification_scan_with_this_device)
                compareEmojiSubtitle = stringProvider.getString(R.string.verification_scan_self_emoji_subtitle)
            } else {
                scanCodeInstructions = stringProvider.getString(R.string.verification_scan_notice)
                scanOtherCodeTitle = stringProvider.getString(R.string.verification_scan_their_code)
                compareEmojiSubtitle = stringProvider.getString(R.string.verification_scan_emoji_subtitle)
            }

            bottomSheetVerificationNoticeItem {
                id("notice")
                notice(scanCodeInstructions.toEpoxyCharSequence())
            }

            if (state.otherCanScanQrCode && !state.qrCodeText.isNullOrBlank()) {
                bottomSheetVerificationQrCodeItem {
                    id("qr")
                    data(state.qrCodeText)
                }

                bottomSheetDividerItem {
                    id("sep0")
                }
            }

            if (state.otherCanShowQrCode) {
                bottomSheetVerificationActionItem {
                    id("openCamera")
                    title(scanOtherCodeTitle)
                    titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    iconRes(R.drawable.ic_camera)
                    iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    listener { host.listener?.openCamera() }
                }

                bottomSheetDividerItem {
                    id("sep1")
                }
            }

            bottomSheetVerificationActionItem {
                id("openEmoji")
                title(host.stringProvider.getString(R.string.verification_scan_emoji_title))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                subTitle(compareEmojiSubtitle)
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { host.listener?.doVerifyBySas() }
            }
        } else if (state.sasModeAvailable) {
            bottomSheetVerificationActionItem {
                id("openEmoji")
                title(host.stringProvider.getString(R.string.verification_no_scan_emoji_title))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { host.listener?.doVerifyBySas() }
            }
        } else if (!state.isReadySent) {
            // a bit of a special case, if you tapped on the timeline cell but not on a button
            buttonPositiveDestructiveButtonBarItem {
                id("accept_decline")
                positiveText(host.stringProvider.getString(R.string.action_accept).toEpoxyCharSequence())
                destructiveText(host.stringProvider.getString(R.string.action_decline).toEpoxyCharSequence())
                positiveButtonClickAction { host.listener?.acceptRequest() }
                destructiveButtonClickAction { host.listener?.declineRequest() }
            }
        }

        if (state.isMe && state.canCrossSign) {
            bottomSheetDividerItem {
                id("sep_notMe")
            }

            bottomSheetVerificationActionItem {
                id("wasnote")
                title(host.stringProvider.getString(R.string.verify_new_session_was_not_me))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                subTitle(host.stringProvider.getString(R.string.verify_new_session_compromized))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                listener { host.listener?.onClickOnWasNotMe() }
            }
        }
    }

    interface Listener {
        fun openCamera()
        fun doVerifyBySas()
        fun onClickOnWasNotMe()
        fun acceptRequest()
        fun declineRequest()
    }
}
