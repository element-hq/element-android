/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.databinding.BottomSheetGenericListWithTitleBinding
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.RoomDetailViewState
import im.vector.app.features.home.room.detail.TimelineViewModel
import im.vector.app.features.navigation.Navigator
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.widgets.model.Widget
import javax.inject.Inject

/**
 * Bottom sheet displaying active widgets in a room.
 */
@AndroidEntryPoint
class RoomWidgetsBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListWithTitleBinding>(),
        RoomWidgetsController.Listener {

    @Inject lateinit var epoxyController: RoomWidgetsController
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var navigator: Navigator

    private val timelineViewModel: TimelineViewModel by parentFragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListWithTitleBinding {
        return BottomSheetGenericListWithTitleBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.bottomSheetRecyclerView.configureWith(epoxyController, hasFixedSize = false)
        views.bottomSheetTitle.text = getString(CommonStrings.active_widgets_title)
        views.bottomSheetTitle.textSize = 20f
        views.bottomSheetTitle.setTextColor(colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        epoxyController.listener = this
        timelineViewModel.onAsync(RoomDetailViewState::activeRoomWidgets) {
            epoxyController.setData(it)
        }
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        epoxyController.listener = null
        super.onDestroyView()
    }

    override fun didSelectWidget(widget: Widget) = withState(timelineViewModel) {
        navigator.openRoomWidget(requireContext(), it.roomId, widget)
        dismiss()
    }

    override fun didSelectManageWidgets() {
        timelineViewModel.handle(RoomDetailAction.OpenIntegrationManager)
        dismiss()
    }

    companion object {
        fun newInstance(): RoomWidgetsBottomSheet {
            return RoomWidgetsBottomSheet()
        }
    }
}
