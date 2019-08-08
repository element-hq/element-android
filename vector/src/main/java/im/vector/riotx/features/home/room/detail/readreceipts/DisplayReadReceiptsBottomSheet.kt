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

package im.vector.riotx.features.home.room.detail.readreceipts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.features.home.room.detail.timeline.action.TimelineEventFragmentArgs
import im.vector.riotx.features.home.room.detail.timeline.action.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.features.home.room.detail.timeline.action.ViewReactionBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
import kotlinx.android.synthetic.main.bottom_sheet_epoxylist_with_title.*
import javax.inject.Inject

/**
 * Bottom sheet displaying list of read receipts for a given event ordered by descending timestamp
 */
class DisplayReadReceiptsBottomSheet : VectorBaseBottomSheetDialogFragment() {

    private val viewModel: DisplayReadReceiptsViewModel by fragmentViewModel()

    @Inject lateinit var displayReadReceiptsViewModelFactory: DisplayReadReceiptsViewModel.Factory
    @Inject lateinit var epoxyController: DisplayReadReceiptsController

    @BindView(R.id.bottom_sheet_display_reactions_list)
    lateinit var epoxyRecyclerView: EpoxyRecyclerView


    override fun injectWith(screenComponent: ScreenComponent) {
        screenComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_epoxylist_with_title, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        epoxyRecyclerView.setController(epoxyController)
        val dividerItemDecoration = DividerItemDecoration(epoxyRecyclerView.context,
                                                          LinearLayout.VERTICAL)
        epoxyRecyclerView.addItemDecoration(dividerItemDecoration)
        bottomSheetTitle.text = getString(R.string.read_receipts_list)
    }


    override fun invalidate() = withState(viewModel) {
        epoxyController.setData(it)
    }

    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData): DisplayReadReceiptsBottomSheet {
            val args = Bundle()
            val parcelableArgs = TimelineEventFragmentArgs(
                    informationData.eventId,
                    roomId,
                    informationData
            )
            args.putParcelable(MvRx.KEY_ARG, parcelableArgs)
            return DisplayReadReceiptsBottomSheet().apply { arguments = args }

        }
    }
}