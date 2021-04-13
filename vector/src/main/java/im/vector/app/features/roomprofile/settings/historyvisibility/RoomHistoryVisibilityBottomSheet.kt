/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings.historyvisibility

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.ui.bottomsheet.BottomSheetGeneric
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import javax.inject.Inject

@Parcelize
data class RoomHistoryVisibilityBottomSheetArgs(
        val currentRoomHistoryVisibility: RoomHistoryVisibility
) : Parcelable

class RoomHistoryVisibilityBottomSheet : BottomSheetGeneric<RoomHistoryVisibilityState, RoomHistoryVisibilityRadioAction>() {

    private lateinit var roomHistoryVisibilitySharedActionViewModel: RoomHistoryVisibilitySharedActionViewModel
    @Inject lateinit var controller: RoomHistoryVisibilityController
    private val viewModel: RoomHistoryVisibilityViewModel by fragmentViewModel(RoomHistoryVisibilityViewModel::class)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getController(): BottomSheetGenericController<RoomHistoryVisibilityState, RoomHistoryVisibilityRadioAction> = controller

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomHistoryVisibilitySharedActionViewModel = activityViewModelProvider.get(RoomHistoryVisibilitySharedActionViewModel::class.java)
    }

    override fun didSelectAction(action: RoomHistoryVisibilityRadioAction) {
        roomHistoryVisibilitySharedActionViewModel.post(action)
        dismiss()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    companion object {
        fun newInstance(currentRoomHistoryVisibility: RoomHistoryVisibility): RoomHistoryVisibilityBottomSheet {
            return RoomHistoryVisibilityBottomSheet().apply {
                setArguments(RoomHistoryVisibilityBottomSheetArgs(currentRoomHistoryVisibility))
            }
        }
    }
}
