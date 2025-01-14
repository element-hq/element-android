/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.user

import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationBigImageItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationEmojisItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationQrCodeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationWaitingItem
import im.vector.app.features.displayname.getBestName
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.QRCodeVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.util.MatrixItem

fun BaseEpoxyVerificationController.verifiedSuccessTile() {
    val host = this
    bottomSheetVerificationNoticeItem {
        id("notice_done")
        notice(
                host.stringProvider.getString(
                        CommonStrings.verification_conclusion_ok_notice
                )
                        .toEpoxyCharSequence()
        )
    }
    bottomSheetVerificationBigImageItem {
        id("image")
        roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Trusted)
    }
}

fun BaseEpoxyVerificationController.bottomDone(listener: ClickListener) {
    val host = this
    bottomSheetDividerItem {
        id("sep_done")
    }

    bottomSheetVerificationActionItem {
        id("done")
        title(host.stringProvider.getString(CommonStrings.done))
        titleColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        iconRes(R.drawable.ic_arrow_right)
        iconColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
//        listener { host.listener?.onDone(true) }
        listener(listener)
    }
}

fun BaseEpoxyVerificationController.gotIt(listener: ClickListener) {
    val host = this
    bottomSheetDividerItem {
        id("sep_gotit")
    }

    bottomSheetVerificationActionItem {
        id("gotit")
        title(host.stringProvider.getString(CommonStrings.action_got_it))
        titleColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        iconRes(R.drawable.ic_arrow_right)
        iconColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        listener(listener)
    }
}

fun BaseEpoxyVerificationController.renderStartTransactionOptions(request: PendingVerificationRequest, isMe: Boolean) {
    val scanCodeInstructions: String
    val scanOtherCodeTitle: String
    val compareEmojiSubtitle: String
    if (isMe) {
        scanCodeInstructions = stringProvider.getString(CommonStrings.verification_scan_self_notice)
        scanOtherCodeTitle = stringProvider.getString(CommonStrings.verification_scan_with_this_device)
        compareEmojiSubtitle = stringProvider.getString(CommonStrings.verification_scan_self_emoji_subtitle)
    } else {
        scanCodeInstructions = stringProvider.getString(CommonStrings.verification_scan_notice)
        scanOtherCodeTitle = stringProvider.getString(CommonStrings.verification_scan_their_code)
        compareEmojiSubtitle = stringProvider.getString(CommonStrings.verification_scan_emoji_subtitle)
    }
    val host = this

    bottomSheetVerificationNoticeItem {
        id("notice")
        notice(scanCodeInstructions.toEpoxyCharSequence())
    }

    if (request.weShouldDisplayQRCode && !request.qrCodeText.isNullOrEmpty()) {
        bottomSheetVerificationQrCodeItem {
            id("qr")
            data(request.qrCodeText!!)
        }

        bottomSheetDividerItem {
            id("sep0")
        }
    }

    if (request.weShouldShowScanOption) {
        bottomSheetVerificationActionItem {
            id("openCamera")
            title(scanOtherCodeTitle)
            titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            iconRes(R.drawable.ic_camera)
            iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            listener { host.listener?.openCamera() }
        }

        bottomSheetDividerItem {
            id("sep1")
        }

        bottomSheetVerificationActionItem {
            id("openEmoji")
            title(host.stringProvider.getString(CommonStrings.verification_scan_emoji_title))
            titleColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
            subTitle(compareEmojiSubtitle)
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
            listener { host.listener?.doVerifyBySas() }
        }
    } else if (request.isSasSupported) {
        bottomSheetVerificationActionItem {
            id("openEmoji")
            title(host.stringProvider.getString(CommonStrings.verification_no_scan_emoji_title))
            titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
            listener { host.listener?.doVerifyBySas() }
        }
    } else {
        // ??? can this happen
    }
}

fun BaseEpoxyVerificationController.renderAcceptDeclineRequest() {
    val host = this
    bottomSheetDividerItem {
        id("sep_accept_Decline")
    }
    bottomSheetVerificationActionItem {
        id("accept_pr")
        title(host.stringProvider.getString(CommonStrings.action_accept))
        titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
        // subTitle(host.stringProvider.getString(CommonStrings.verification_request_start_notice))
        iconRes(R.drawable.ic_arrow_right)
        iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
        listener { host.listener?.acceptRequest() }
    }
    bottomSheetVerificationActionItem {
        id("decline_pr")
        title(host.stringProvider.getString(CommonStrings.action_decline))
        titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
        // subTitle(host.stringProvider.getString(CommonStrings.verification_request_start_notice))
        iconRes(R.drawable.ic_arrow_right)
        iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
        listener { host.listener?.declineRequest() }
    }
}

fun BaseEpoxyVerificationController.renderCancel(cancelCode: CancelCode) {
    val host = this
    when (cancelCode) {
        CancelCode.QrCodeInvalid -> {
            // TODO
        }
        CancelCode.MismatchedUser,
        CancelCode.MismatchedSas,
        CancelCode.MismatchedCommitment,
        CancelCode.MismatchedKeys -> {
            bottomSheetVerificationNoticeItem {
                id("notice")
                notice(host.stringProvider.getString(CommonStrings.verification_conclusion_not_secure).toEpoxyCharSequence())
            }

            bottomSheetVerificationBigImageItem {
                id("image")
                roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Warning)
            }

            bottomSheetVerificationNoticeItem {
                id("warning_notice")
                notice(host.eventHtmlRenderer.render(host.stringProvider.getString(CommonStrings.verification_conclusion_compromised)).toEpoxyCharSequence())
            }
        }
        else -> {
            bottomSheetVerificationNoticeItem {
                id("notice_cancelled")
                notice(host.stringProvider.getString(CommonStrings.verify_cancelled_notice).toEpoxyCharSequence())
            }
        }
    }
}

fun BaseEpoxyVerificationController.buildEmojiItem(emoji: List<EmojiRepresentation>) {
    val host = this
    bottomSheetVerificationNoticeItem {
        id("notice")
        notice(host.stringProvider.getString(CommonStrings.verification_emoji_notice).toEpoxyCharSequence())
    }

    bottomSheetVerificationEmojisItem {
        id("emojis")
        emojiRepresentation0(emoji[0])
        emojiRepresentation1(emoji[1])
        emojiRepresentation2(emoji[2])
        emojiRepresentation3(emoji[3])
        emojiRepresentation4(emoji[4])
        emojiRepresentation5(emoji[5])
        emojiRepresentation6(emoji[6])
    }

    buildSasCodeActions()
}

fun BaseEpoxyVerificationController.buildSasCodeActions() {
    val host = this
    bottomSheetDividerItem {
        id("sepsas0")
    }
    bottomSheetVerificationActionItem {
        id("ko")
        title(host.stringProvider.getString(CommonStrings.verification_sas_do_not_match))
        titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
        iconRes(R.drawable.ic_check_off)
        iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
        listener { host.listener?.onDoNotMatchButtonTapped() }
    }
    bottomSheetDividerItem {
        id("sepsas1")
    }
    bottomSheetVerificationActionItem {
        id("ok")
        title(host.stringProvider.getString(CommonStrings.verification_sas_match))
        titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
        iconRes(R.drawable.ic_check_on)
        iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
        listener { host.listener?.onMatchButtonTapped() }
    }
}

fun BaseEpoxyVerificationController.renderSasTransaction(transaction: VerificationTransactionData.SasTransactionData) {
    val host = this
    when (val txState = transaction.state) {
        SasTransactionState.SasShortCodeReady -> {
            buildEmojiItem(transaction.emojiCodeRepresentation.orEmpty())
        }
        is SasTransactionState.SasMacReceived -> {
            if (!txState.codeConfirmed) {
                buildEmojiItem(transaction.emojiCodeRepresentation.orEmpty())
            } else {
                // waiting
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(host.stringProvider.getString(CommonStrings.please_wait))
                }
            }
        }
        is SasTransactionState.Cancelled,
        is SasTransactionState.Done -> {
            // should show request status
        }
        else -> {
            // waiting
            bottomSheetVerificationWaitingItem {
                id("waiting")
                title(host.stringProvider.getString(CommonStrings.please_wait))
            }
        }
    }
}

fun BaseEpoxyVerificationController.renderQrTransaction(transaction: VerificationTransactionData.QrTransactionData, otherUserItem: MatrixItem?) {
    val host = this
    when (transaction.state) {
        QRCodeVerificationState.Reciprocated -> {
            // we are waiting for confirmation from the other side
            bottomSheetVerificationNoticeItem {
                id("notice")
                apply {
                    notice(host.stringProvider.getString(CommonStrings.qr_code_scanned_verif_waiting_notice).toEpoxyCharSequence())
                }
            }

            bottomSheetVerificationBigImageItem {
                id("image")
                roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Trusted)
            }

            bottomSheetVerificationWaitingItem {
                id("waiting")
                if (otherUserItem != null) {
                    title(host.stringProvider.getString(CommonStrings.qr_code_scanned_verif_waiting, otherUserItem.getBestName()))
                } else {
                    title(host.stringProvider.getString(CommonStrings.qr_code_scanned_verif_waiting, transaction.otherDeviceId.orEmpty()))
                }
            }
        }
        QRCodeVerificationState.WaitingForScanConfirmation -> {
            // we need to confirm that the other party actual scanned us
            bottomSheetVerificationNoticeItem {
                id("notice")
                apply {
                    if (otherUserItem != null) {
                        val name = otherUserItem.getBestName()
                        notice(host.stringProvider.getString(CommonStrings.qr_code_scanned_by_other_notice, name).toEpoxyCharSequence())
                    } else {
                        notice(host.stringProvider.getString(CommonStrings.qr_code_scanned_self_verif_notice).toEpoxyCharSequence())
                    }
                }
            }

            bottomSheetVerificationBigImageItem {
                id("image")
                roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Trusted)
            }

            bottomSheetDividerItem {
                id("sep0")
            }

            bottomSheetVerificationActionItem {
                id("deny")
                title(host.stringProvider.getString(CommonStrings.qr_code_scanned_by_other_no))
                titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
                iconRes(R.drawable.ic_check_off)
                iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
                listener { host.listener?.onUserDeniesQrCodeScanned() }
            }

            bottomSheetDividerItem {
                id("sep1")
            }

            bottomSheetVerificationActionItem {
                id("confirm")
                title(host.stringProvider.getString(CommonStrings.qr_code_scanned_by_other_yes))
                titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                iconRes(R.drawable.ic_check_on)
                iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                listener { host.listener?.onUserConfirmsQrCodeScanned() }
            }
        }
        QRCodeVerificationState.WaitingForOtherDone -> {
            bottomSheetVerificationNoticeItem {
                id("notice")
                apply {
                    notice(host.stringProvider.getString(CommonStrings.qr_code_scanned_verif_waiting_notice).toEpoxyCharSequence())
                }
            }

            bottomSheetVerificationBigImageItem {
                id("image")
                roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Trusted)
            }

            bottomSheetVerificationWaitingItem {
                id("waiting")
                apply {
                    if (otherUserItem != null) {
                        title(host.stringProvider.getString(CommonStrings.qr_code_scanned_verif_waiting, otherUserItem.getBestName()))
                    } else {
                        title(host.stringProvider.getString(CommonStrings.qr_code_scanned_verif_waiting, transaction.otherDeviceId.orEmpty()))
                    }
                }
            }
        }
        QRCodeVerificationState.Done -> {
            // Done
        }
        QRCodeVerificationState.Cancelled -> {
            // Done
//                renderCancel(transaction.)
        }
    }
}

class VerificationEpoxyExt
