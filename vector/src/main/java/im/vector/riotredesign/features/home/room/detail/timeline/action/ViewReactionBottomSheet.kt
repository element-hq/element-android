/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotredesign.EmojiCompatFontProvider
import im.vector.riotredesign.R
import im.vector.riotredesign.core.di.ScreenComponent
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import kotlinx.android.synthetic.main.bottom_sheet_display_reactions.*
import javax.inject.Inject

/**
 * Bottom sheet displaying list of reactions for a given event ordered by timestamp
 */
class ViewReactionBottomSheet : VectorBaseBottomSheetDialogFragment() {

    private val viewModel: ViewReactionViewModel by fragmentViewModel(ViewReactionViewModel::class)

    @Inject lateinit var viewReactionViewModelFactory: ViewReactionViewModel.Factory
    @Inject lateinit var emojiCompatFontProvider: EmojiCompatFontProvider

    @BindView(R.id.bottom_sheet_display_reactions_list)
    lateinit var epoxyRecyclerView: EpoxyRecyclerView

    private val epoxyController by lazy { ViewReactionsEpoxyController(emojiCompatFontProvider.typeface) }

    override fun injectWith(screenComponent: ScreenComponent) {
        screenComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_display_reactions, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        epoxyRecyclerView.setController(epoxyController)
        val dividerItemDecoration = DividerItemDecoration(epoxyRecyclerView.context,
                LinearLayout.VERTICAL)
        epoxyRecyclerView.addItemDecoration(dividerItemDecoration)
    }


    override fun invalidate() = withState(viewModel) {
        if (it.mapReactionKeyToMemberList() == null) {
            bottomSheetViewReactionSpinner.isVisible = true
            bottomSheetViewReactionSpinner.animate()
        } else {
            bottomSheetViewReactionSpinner.isVisible = false
        }
        epoxyController.setData(it)
    }

    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData): ViewReactionBottomSheet {
            val args = Bundle()
            val parcelableArgs = TimelineEventFragmentArgs(
                    informationData.eventId,
                    roomId,
                    informationData
            )
            args.putParcelable(MvRx.KEY_ARG, parcelableArgs)
            return ViewReactionBottomSheet().apply { arguments = args }

        }
    }
}