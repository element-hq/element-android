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

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.OnClick
import com.airbnb.mvrx.*
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.platform.parentFragmentViewModel
import kotlinx.android.synthetic.main.fragment_bottom_sas_verification_code.*
import javax.inject.Inject

class SASVerificationCodeFragment @Inject constructor(
        val viewModelFactory: SASVerificationCodeViewModel.Factory
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_bottom_sas_verification_code

    @BindView(R.id.sas_emoji_grid)
    lateinit var emojiGrid: ViewGroup


    @BindView(R.id.sas_decimal_code)
    lateinit var decimalTextView: TextView

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


    private val viewModel by fragmentViewModel(SASVerificationCodeViewModel::class)

    private val sharedViewModel by parentFragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun invalidate() = withState(viewModel) { state ->

        if (state.supportsEmoji) {
            decimalTextView.isVisible = false
            when(val emojiDescription = state.emojiDescription) {
                is Success -> {
                    sasLoadingProgress.isVisible = false
                    emojiGrid.isVisible = true
                    ButtonsVisibilityGroup.isVisible = true
                    emojiDescription.invoke().forEachIndexed { index, emojiRepresentation ->
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

                    if (state.isWaitingFromOther) {
                        //hide buttons
                        ButtonsVisibilityGroup.isInvisible = true
                        sasCodeWaitingPartnerText.isVisible = true
                    } else {
                        ButtonsVisibilityGroup.isVisible = true
                        sasCodeWaitingPartnerText.isVisible = false
                    }

                }
                is Fail -> {
                    sasLoadingProgress.isVisible = false
                    emojiGrid.isInvisible = true
                    ButtonsVisibilityGroup.isInvisible = true
                    //TODO?
                }
                else -> {
                    sasLoadingProgress.isVisible = true
                    emojiGrid.isInvisible = true
                    ButtonsVisibilityGroup.isInvisible = true
                }
            }
        } else {
            //Decimal
            emojiGrid.isInvisible = true
            decimalTextView.isVisible = true
            val decimalCode = state.decimalDescription.invoke()
            decimalTextView.text = decimalCode

            //TODO
            if (state.isWaitingFromOther) {
                //hide buttons
                ButtonsVisibilityGroup.isInvisible = true
                sasCodeWaitingPartnerText.isVisible = true
            } else {
                ButtonsVisibilityGroup.isVisible = decimalCode != null
                sasCodeWaitingPartnerText.isVisible = false
            }
        }
    }


    @OnClick(R.id.sas_request_continue_button)
    fun onMatchButtonTapped() = withState(viewModel) { state ->
        //UX echo
        ButtonsVisibilityGroup.isInvisible = true
        sasCodeWaitingPartnerText.isVisible = true
        sharedViewModel.handle(VerificationAction.SASMatchAction(state.otherUserId, state.transactionId))
    }

    @OnClick(R.id.sas_request_cancel_button)
    fun onDoNotMatchButtonTapped() = withState(viewModel) { state ->
        //UX echo
        ButtonsVisibilityGroup.isInvisible = true
        sasCodeWaitingPartnerText.isVisible = true
        sharedViewModel.handle(VerificationAction.SASDoNotMatchAction(state.otherUserId, state.transactionId))
    }

}
