/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.crypto.verification.emoji

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetSeparatorItem
import im.vector.riotx.core.epoxy.errorWithRetryItem
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.crypto.verification.epoxy.*
import javax.inject.Inject

class VerificationEmojiCodeController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val errorFormatter: ErrorFormatter
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationEmojiCodeViewState? = null

    init {
        // We are requesting a model build directly as the first build of epoxy is on the main thread.
        // It avoids to build the whole list on the main thread.
        requestModelBuild()
    }

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
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(stringProvider.getString(R.string.verification_emoji_notice))
        }

        when (val emojiDescription = state.emojiDescription) {
            is Success -> {
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
            is Fail    -> {
                errorWithRetryItem {
                    id("error")
                    text(errorFormatter.toHumanReadable(emojiDescription.error))
                }
            }
            else       -> {
                bottomSheetVerificationWaitingItem {
                    id("waiting")
                    title(stringProvider.getString(R.string.please_wait))
                }
            }
        }
    }

    private fun buildDecimal(state: VerificationEmojiCodeViewState) {
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(stringProvider.getString(R.string.verification_code_notice))
        }

        bottomSheetVerificationDecimalCodeItem {
            id("decimal")
            code(state.decimalDescription.invoke() ?: "")
        }

        buildActions(state)
    }

    private fun buildActions(state: VerificationEmojiCodeViewState) {
        bottomSheetSeparatorItem {
            id("sep0")
        }

        if (state.isWaitingFromOther) {
            bottomSheetVerificationWaitingItem {
                id("waiting")
                title(stringProvider.getString(R.string.verification_request_waiting_for, state.otherUser?.getBestName() ?: ""))
            }
        } else {
            bottomSheetVerificationActionItem {
                id("ko")
                title(stringProvider.getString(R.string.verification_sas_do_not_match))
                titleColor(colorProvider.getColor(R.color.vector_error_color))
                iconRes(R.drawable.ic_check_off)
                iconColor(colorProvider.getColor(R.color.vector_error_color))
                listener { listener?.onDoNotMatchButtonTapped() }
            }
            bottomSheetSeparatorItem {
                id("sep1")
            }
            bottomSheetVerificationActionItem {
                id("ok")
                title(stringProvider.getString(R.string.verification_sas_match))
                titleColor(colorProvider.getColor(R.color.riotx_accent))
                iconRes(R.drawable.ic_check_on)
                iconColor(colorProvider.getColor(R.color.riotx_accent))
                listener { listener?.onMatchButtonTapped() }
            }
        }
    }

    interface Listener {
        fun onDoNotMatchButtonTapped()
        fun onMatchButtonTapped()
    }
}
