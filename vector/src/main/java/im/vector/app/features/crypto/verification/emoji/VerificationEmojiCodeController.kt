/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.emoji

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationDecimalCodeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationEmojisItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationWaitingItem
import im.vector.app.features.displayname.getBestName
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import javax.inject.Inject

class VerificationEmojiCodeController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val errorFormatter: ErrorFormatter
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationEmojiCodeViewState? = null

    fun update(viewState: VerificationEmojiCodeViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val state = viewState ?: return

        if (state.supportsEmoji) {
            buildEmojiItem(state)
        } else {
            buildDecimal(state)
        }
    }

    private fun buildEmojiItem(state: VerificationEmojiCodeViewState) {
        val host = this
        when (val emojiDescription = state.emojiDescription) {
            is Success -> {
                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(host.stringProvider.getString(R.string.verification_emoji_notice).toEpoxyCharSequence())
                }

                bottomSheetVerificationEmojisItem {
                    id("emojis")
                    emojiRepresentation0(emojiDescription()[0])
                    emojiRepresentation1(emojiDescription()[1])
                    emojiRepresentation2(emojiDescription()[2])
                    emojiRepresentation3(emojiDescription()[3])
                    emojiRepresentation4(emojiDescription()[4])
                    emojiRepresentation5(emojiDescription()[5])
                    emojiRepresentation6(emojiDescription()[6])
                }

                buildActions(state)
            }
            is Fail -> {
                errorWithRetryItem {
                    id("error")
                    text(host.errorFormatter.toHumanReadable(emojiDescription.error))
                }
            }
            else -> {
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(host.stringProvider.getString(R.string.please_wait))
                }
            }
        }
    }

    private fun buildDecimal(state: VerificationEmojiCodeViewState) {
        val host = this
        when (val decimalDescription = state.decimalDescription) {
            is Success -> {
                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(host.stringProvider.getString(R.string.verification_code_notice).toEpoxyCharSequence())
                }

                bottomSheetVerificationDecimalCodeItem {
                    id("decimal")
                    code(state.decimalDescription.invoke() ?: "")
                }

                buildActions(state)
            }
            is Fail -> {
                errorWithRetryItem {
                    id("error")
                    text(host.errorFormatter.toHumanReadable(decimalDescription.error))
                }
            }
            else -> {
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(host.stringProvider.getString(R.string.please_wait))
                }
            }
        }
    }

    private fun buildActions(state: VerificationEmojiCodeViewState) {
        val host = this
        bottomSheetDividerItem {
            id("sep0")
        }

        if (state.isWaitingFromOther) {
            bottomSheetVerificationWaitingItem {
                id("waiting")
                title(host.stringProvider.getString(R.string.verification_request_waiting_for, state.otherUser.getBestName()))
            }
        } else {
            bottomSheetVerificationActionItem {
                id("ko")
                title(host.stringProvider.getString(R.string.verification_sas_do_not_match))
                titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                iconRes(R.drawable.ic_check_off)
                iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                listener { host.listener?.onDoNotMatchButtonTapped() }
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
                listener { host.listener?.onMatchButtonTapped() }
            }
        }
    }

    interface Listener {
        fun onDoNotMatchButtonTapped()
        fun onMatchButtonTapped()
    }
}
