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

package im.vector.app.features.crypto.verification.transaction

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationBigImageItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationEmojisItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationWaitingItem
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import javax.inject.Inject

class VerificationTransactionController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val eventHtmlRenderer: EventHtmlRenderer,
) : EpoxyController() {

    var aTransaction: Async<VerificationTransaction>? = null

    fun update(asyncTransaction: Async<VerificationTransaction>) {
        this.aTransaction = asyncTransaction
        requestModelBuild()
    }

    override fun buildModels() {
        val host = this
        when (aTransaction) {
            null,
            Uninitialized -> {
                // empty
            }
            is Fail -> {
            }
            is Loading -> {
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(host.stringProvider.getString(R.string.please_wait))
                }
            }
            is Success -> {
                val tx = aTransaction?.invoke() ?: return
                if (tx is SasVerificationTransaction) {
                    when (val txState = tx.state) {
                        VerificationTxState.SasShortCodeReady -> {
                            buildEmojiItem(tx.getEmojiCodeRepresentation())
                        }
                        is VerificationTxState.Cancelled -> {
                            renderCancel(txState.cancelCode)
                        }
                        is VerificationTxState.Done -> {

                        }
                        else -> {
                            // waiting
                            bottomSheetVerificationWaitingItem {
                                id("waiting")
                                title(host.stringProvider.getString(R.string.please_wait))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderCancel(cancelCode: CancelCode) {
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
                    notice(host.stringProvider.getString(R.string.verification_conclusion_not_secure).toEpoxyCharSequence())
                }

                bottomSheetVerificationBigImageItem {
                    id("image")
                    roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Warning)
                }

                bottomSheetVerificationNoticeItem {
                    id("warning_notice")
                    notice(host.eventHtmlRenderer.render(host.stringProvider.getString(R.string.verification_conclusion_compromised)).toEpoxyCharSequence())
                }
            }
            else -> {
                bottomSheetVerificationNoticeItem {
                    id("notice_cancelled")
                    notice(host.stringProvider.getString(R.string.verify_cancelled_notice).toEpoxyCharSequence())
                }
            }
        }
    }

    private fun buildEmojiItem(emoji: List<EmojiRepresentation>) {
        val host = this
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(host.stringProvider.getString(R.string.verification_emoji_notice).toEpoxyCharSequence())
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

    private fun buildSasCodeActions() {
        val host = this
        bottomSheetDividerItem {
            id("sep0")
        }
        bottomSheetVerificationActionItem {
            id("ko")
            title(host.stringProvider.getString(R.string.verification_sas_do_not_match))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
            iconRes(R.drawable.ic_check_off)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
            // listener { host.listener?.onDoNotMatchButtonTapped() }
        }
        bottomSheetDividerItem {
            id("sep1")
        }
        bottomSheetVerificationActionItem {
            id("ok")
            title(host.stringProvider.getString(R.string.verification_sas_match))
            titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
            iconRes(R.drawable.ic_check_on)
            iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
            // listener { host.listener?.onMatchButtonTapped() }
        }
    }
}
