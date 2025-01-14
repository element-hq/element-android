/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetCallControlsBinding
import im.vector.app.features.VectorFeatures
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class CallControlsBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetCallControlsBinding>() {
    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCallControlsBinding {
        return BottomSheetCallControlsBinding.inflate(inflater, container, false)
    }

    @Inject lateinit var vectorFeatures: VectorFeatures

    private val callViewModel: VectorCallViewModel by activityViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callViewModel.onEach {
            renderState(it)
        }

        views.callControlsSwitchCamera.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleCamera)
            dismiss()
        }

        views.callControlsToggleSDHD.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleHDSD)
            dismiss()
        }

        views.callControlsToggleHoldResume.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleHoldResume)
            dismiss()
        }

        views.callControlsOpenDialPad.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.OpenDialPad)
        }

        views.callControlsTransfer.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.InitiateCallTransfer)
            dismiss()
        }

        views.callControlsShareScreen.isVisible = vectorFeatures.isScreenSharingEnabled()
        views.callControlsShareScreen.views.bottomSheetActionClickableZone.debouncedClicks {
            callViewModel.handle(VectorCallViewActions.ToggleScreenSharing)
            dismiss()
        }
    }

    private fun renderState(state: VectorCallViewState) {
        views.callControlsSwitchCamera.isVisible = state.isVideoCall && state.canSwitchCamera
        views.callControlsSwitchCamera.subTitle = getString(if (state.isFrontCamera) CommonStrings.call_camera_front else CommonStrings.call_camera_back)
        if (state.isVideoCall) {
            views.callControlsToggleSDHD.isVisible = true
            if (state.isHD) {
                views.callControlsToggleSDHD.title = getString(CommonStrings.call_format_turn_hd_off)
                views.callControlsToggleSDHD.subTitle = null
                views.callControlsToggleSDHD.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_hd_disabled)
            } else {
                views.callControlsToggleSDHD.title = getString(CommonStrings.call_format_turn_hd_on)
                views.callControlsToggleSDHD.subTitle = null
                views.callControlsToggleSDHD.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_hd)
            }
        } else {
            views.callControlsToggleSDHD.isVisible = false
        }
        if (state.isRemoteOnHold) {
            views.callControlsToggleHoldResume.title = getString(CommonStrings.call_resume_action)
            views.callControlsToggleHoldResume.subTitle = null
            views.callControlsToggleHoldResume.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_call_resume_action)
        } else {
            views.callControlsToggleHoldResume.title = getString(CommonStrings.call_hold_action)
            views.callControlsToggleHoldResume.subTitle = null
            views.callControlsToggleHoldResume.leftIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_call_hold_action)
        }
        views.callControlsTransfer.isVisible = state.canOpponentBeTransferred
        views.callControlsShareScreen.title = getString(
                if (state.isSharingScreen) CommonStrings.call_stop_screen_sharing else CommonStrings.call_start_screen_sharing
        )
    }
}
