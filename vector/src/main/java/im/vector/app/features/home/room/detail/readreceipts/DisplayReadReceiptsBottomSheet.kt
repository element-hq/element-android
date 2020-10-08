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

package im.vector.app.features.home.room.detail.readreceipts

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.features.home.room.detail.timeline.action.EventSharedAction
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.bottom_sheet_generic_list_with_title.*
import javax.inject.Inject

@Parcelize
data class DisplayReadReceiptArgs(
        val readReceipts: List<ReadReceiptData>
) : Parcelable

/**
 * Bottom sheet displaying list of read receipts for a given event ordered by descending timestamp
 */
class DisplayReadReceiptsBottomSheet : VectorBaseBottomSheetDialogFragment(), DisplayReadReceiptsController.Listener {

    @Inject lateinit var epoxyController: DisplayReadReceiptsController

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    private val displayReadReceiptArgs: DisplayReadReceiptArgs by args()

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutResId() = R.layout.bottom_sheet_generic_list_with_title

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        recyclerView.configureWith(epoxyController, hasFixedSize = false)
        bottomSheetTitle.text = getString(R.string.seen_by)
        epoxyController.listener = this
        epoxyController.setData(displayReadReceiptArgs.readReceipts)
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        epoxyController.listener = null
        super.onDestroyView()
    }

    override fun didSelectUser(userId: String) {
        sharedActionViewModel.post(EventSharedAction.OpenUserProfile(userId))
    }

    // we are not using state for this one as it's static, so no need to override invalidate()

    companion object {
        fun newInstance(readReceipts: List<ReadReceiptData>): DisplayReadReceiptsBottomSheet {
            val args = Bundle()
            val parcelableArgs = DisplayReadReceiptArgs(
                    readReceipts
            )
            args.putParcelable(MvRx.KEY_ARG, parcelableArgs)
            return DisplayReadReceiptsBottomSheet().apply { arguments = args }
        }
    }
}
