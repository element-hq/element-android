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

package im.vector.app.features.crypto.verification.conclusion

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationBigImageItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.html.EventHtmlRenderer
import javax.inject.Inject

class VerificationConclusionController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val eventHtmlRenderer: EventHtmlRenderer
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationConclusionViewState? = null

    fun update(viewState: VerificationConclusionViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val state = viewState ?: return

        when (state.conclusionState) {
            ConclusionState.SUCCESS -> {
                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(stringProvider.getString(
                            if (state.isSelfVerification) R.string.verification_conclusion_ok_self_notice
                            else R.string.verification_conclusion_ok_notice))
                }

                bottomSheetVerificationBigImageItem {
                    id("image")
                    imageRes(R.drawable.ic_shield_trusted)
                }

                bottomDone()
            }
            ConclusionState.WARNING -> {
                bottomSheetVerificationNoticeItem {
                    id("notice")
                    notice(stringProvider.getString(R.string.verification_conclusion_not_secure))
                }

                bottomSheetVerificationBigImageItem {
                    id("image")
                    imageRes(R.drawable.ic_shield_warning)
                }

                bottomSheetVerificationNoticeItem {
                    id("warning_notice")
                    notice(eventHtmlRenderer.render(stringProvider.getString(R.string.verification_conclusion_compromised)))
                }

                bottomDone()
            }
            ConclusionState.CANCELLED -> {
                bottomSheetVerificationNoticeItem {
                    id("notice_cancelled")
                    notice(stringProvider.getString(R.string.verify_cancelled_notice))
                }

                dividerItem {
                    id("sep0")
                }

                bottomSheetVerificationActionItem {
                    id("got_it")
                    title(stringProvider.getString(R.string.sas_got_it))
                    titleColor(colorProvider.getColor(R.color.riotx_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_accent))
                    listener { listener?.onButtonTapped() }
                }
            }
        }
    }

    private fun bottomDone() {
        dividerItem {
            id("sep0")
        }

        bottomSheetVerificationActionItem {
            id("done")
            title(stringProvider.getString(R.string.done))
            titleColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
            listener { listener?.onButtonTapped() }
        }
    }

    interface Listener {
        fun onButtonTapped()
    }
}
