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

package im.vector.app.features.home.room.list.actions

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import im.vector.app.features.navigation.Navigator
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class RoomListActionsArgs(
        val roomId: String,
        val mode: Mode
) : Parcelable {

    enum class Mode {
        FULL,
        NOTIFICATIONS
    }
}

/**
 * Bottom sheet fragment that shows room information with list of contextual actions
 */
class RoomListQuickActionsBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListBinding>(),
        RoomListQuickActionsEpoxyController.Listener {

    private lateinit var sharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    @Inject lateinit var sharedViewPool: RecyclerView.RecycledViewPool
    @Inject lateinit var roomListActionsViewModelFactory: RoomListQuickActionsViewModel.Factory
    @Inject lateinit var roomListActionsEpoxyController: RoomListQuickActionsEpoxyController
    @Inject lateinit var navigator: Navigator

    private val viewModel: RoomListQuickActionsViewModel by fragmentViewModel(RoomListQuickActionsViewModel::class)

    override val showExpanded = true

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListBinding {
        return BottomSheetGenericListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        views.bottomSheetRecyclerView.configureWith(
                epoxyController = roomListActionsEpoxyController,
                viewPool = sharedViewPool,
                hasFixedSize = false,
                disableItemAnimation = true
        )
        roomListActionsEpoxyController.listener = this
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        roomListActionsEpoxyController.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        roomListActionsEpoxyController.setData(it)
        super.invalidate()
    }

    override fun didSelectMenuAction(quickAction: RoomListQuickActionsSharedAction) {
        sharedActionViewModel.post(quickAction)
        // Do not dismiss for all the actions
        when (quickAction) {
            is RoomListQuickActionsSharedAction.LowPriority -> Unit
            is RoomListQuickActionsSharedAction.Favorite    -> Unit
            else                                            -> dismiss()
        }
    }

    companion object {
        fun newInstance(roomId: String, mode: RoomListActionsArgs.Mode): RoomListQuickActionsBottomSheet {
            return RoomListQuickActionsBottomSheet().apply {
                setArguments(RoomListActionsArgs(roomId, mode))
            }
        }
    }
}
