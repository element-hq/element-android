/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.crypto.verification

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import butterknife.BindView
import butterknife.OnClick
import im.vector.matrix.android.api.session.crypto.sas.IncomingSasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.OutgoingSasVerificationRequest
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import javax.inject.Inject

class SASVerificationShortCodeFragment @Inject constructor(): VectorBaseFragment() {

    private lateinit var viewModel: SasVerificationViewModel

    @BindView(R.id.sas_decimal_code)
    lateinit var decimalTextView: TextView

    @BindView(R.id.sas_emoji_description)
    lateinit var descriptionTextView: TextView

    @BindView(R.id.sas_emoji_grid)
    lateinit var emojiGrid: ViewGroup

    @BindView(R.id.emoji0)
    lateinit var emoji0View: ViewGroup
    @BindView(R.id.emoji1)
    lateinit var emoji1View: ViewGroup
    @BindView(R.id.emoji2)
    lateinit var emoji2View: ViewGroup
    @BindView(R.id.emoji3)
    lateinit var emoji3View: ViewGroup
    @BindView(R.id.emoji4)
    lateinit var emoji4View: ViewGroup
    @BindView(R.id.emoji5)
    lateinit var emoji5View: ViewGroup
    @BindView(R.id.emoji6)
    lateinit var emoji6View: ViewGroup

    override fun getLayoutResId() = R.layout.fragment_sas_verification_display_code

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activityViewModelProvider.get(SasVerificationViewModel::class.java)

        viewModel.transaction?.let {
            if (it.supportsEmoji()) {
                val emojicodes = it.getEmojiCodeRepresentation()
                emojicodes.forEachIndexed { index, emojiRepresentation ->
                    when (index) {
                        0 -> {
                            emoji0View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji0View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation.nameResId)
                        }
                        1 -> {
                            emoji1View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji1View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation.nameResId)
                        }
                        2 -> {
                            emoji2View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji2View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation.nameResId)
                        }
                        3 -> {
                            emoji3View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji3View.findViewById<TextView>(R.id.item_emoji_name_tv)?.setText(emojiRepresentation.nameResId)
                        }
                        4 -> {
                            emoji4View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji4View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation.nameResId)
                        }
                        5 -> {
                            emoji5View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji5View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation.nameResId)
                        }
                        6 -> {
                            emoji6View.findViewById<TextView>(R.id.item_emoji_tv).text = emojiRepresentation.emoji
                            emoji6View.findViewById<TextView>(R.id.item_emoji_name_tv).setText(emojiRepresentation.nameResId)
                        }
                    }
                }
            }

            // decimal is at least supported
            decimalTextView.text = it.getDecimalCodeRepresentation()

            if (it.supportsEmoji()) {
                descriptionTextView.text = getString(R.string.sas_emoji_description)
                decimalTextView.isVisible = false
                emojiGrid.isVisible = true
            } else {
                descriptionTextView.text = getString(R.string.sas_decimal_description)
                decimalTextView.isVisible = true
                emojiGrid.isInvisible = true
            }
        }

        viewModel.transactionState.observe(viewLifecycleOwner, Observer {
            if (viewModel.transaction is IncomingSasVerificationTransaction) {
                val uxState = (viewModel.transaction as IncomingSasVerificationTransaction).uxState
                when (uxState) {
                    IncomingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        viewModel.loadingLiveEvent.value = null
                    }
                    IncomingSasVerificationTransaction.UxState.VERIFIED -> {
                        viewModel.loadingLiveEvent.value = null
                        viewModel.deviceIsVerified()
                    }
                    IncomingSasVerificationTransaction.UxState.CANCELLED_BY_ME,
                    IncomingSasVerificationTransaction.UxState.CANCELLED_BY_OTHER -> {
                        viewModel.loadingLiveEvent.value = null
                        viewModel.navigateCancel()
                    }
                    else -> {
                        viewModel.loadingLiveEvent.value = R.string.sas_waiting_for_partner
                    }
                }
            } else if (viewModel.transaction is OutgoingSasVerificationRequest) {
                val uxState = (viewModel.transaction as OutgoingSasVerificationRequest).uxState
                when (uxState) {
                    OutgoingSasVerificationRequest.UxState.SHOW_SAS -> {
                        viewModel.loadingLiveEvent.value = null
                    }
                    OutgoingSasVerificationRequest.UxState.VERIFIED -> {
                        viewModel.loadingLiveEvent.value = null
                        viewModel.deviceIsVerified()
                    }
                    OutgoingSasVerificationRequest.UxState.CANCELLED_BY_ME,
                    OutgoingSasVerificationRequest.UxState.CANCELLED_BY_OTHER -> {
                        viewModel.loadingLiveEvent.value = null
                        viewModel.navigateCancel()
                    }
                    else -> {
                        viewModel.loadingLiveEvent.value = R.string.sas_waiting_for_partner
                    }
                }
            }
        })
    }

    @OnClick(R.id.sas_request_continue_button)
    fun didAccept() {
        viewModel.confirmEmojiSame()
    }

    @OnClick(R.id.sas_request_cancel_button)
    fun didCancel() {
        viewModel.cancelTransaction()
    }
}
