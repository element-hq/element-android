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

package im.vector.app.features.home.room.detail.widget

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.RoomDetailViewModel
import im.vector.app.features.home.room.detail.RoomDetailViewState
import im.vector.app.features.navigation.Navigator
import org.matrix.android.sdk.api.session.widgets.model.Widget
import kotlinx.android.synthetic.main.bottom_sheet_generic_list_with_title.*
import javax.inject.Inject

/**
 * Bottom sheet displaying active widgets in a room
 */
class RoomWidgetsBottomSheet : VectorBaseBottomSheetDialogFragment(), RoomWidgetsController.Listener {

    @Inject lateinit var epoxyController: RoomWidgetsController
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var navigator: Navigator

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    private val roomDetailViewModel: RoomDetailViewModel by parentFragmentViewModel()

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutResId() = R.layout.bottom_sheet_generic_list_with_title

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerView.configureWith(epoxyController, hasFixedSize = false)
        bottomSheetTitle.text = getString(R.string.active_widgets_title)
        bottomSheetTitle.textSize = 20f
        bottomSheetTitle.setTextColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
        epoxyController.listener = this
        roomDetailViewModel.asyncSubscribe(this, RoomDetailViewState::activeRoomWidgets) {
            epoxyController.setData(it)
        }
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        epoxyController.listener = null
        super.onDestroyView()
    }

    override fun didSelectWidget(widget: Widget) = withState(roomDetailViewModel) {
        navigator.openRoomWidget(requireContext(), it.roomId, widget)
        dismiss()
    }

    override fun didSelectManageWidgets() {
        roomDetailViewModel.handle(RoomDetailAction.OpenIntegrationManager)
        dismiss()
    }

    companion object {
        fun newInstance(): RoomWidgetsBottomSheet {
            return RoomWidgetsBottomSheet()
        }
    }
}
