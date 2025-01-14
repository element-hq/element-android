/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetLiveLocationLabsFlagPromotionBinding

/**
 * Bottom sheet to warn users that feature is still in active development. Users are able to enable labs flag by using the switch in this bottom sheet.
 * This should not be shown if the user already enabled the labs flag.
 */
class LiveLocationLabsFlagPromotionBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetLiveLocationLabsFlagPromotionBinding>() {

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetLiveLocationLabsFlagPromotionBinding {
        return BottomSheetLiveLocationLabsFlagPromotionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOkButton()
    }

    private fun initOkButton() {
        views.promoteLiveLocationFlagOkButton.debouncedClicks {
            val enableLabsFlag = views.promoteLiveLocationFlagSwitch.isChecked
            setFragmentResult(REQUEST_KEY, Bundle().apply {
                putBoolean(BUNDLE_KEY_LABS_APPROVAL, enableLabsFlag)
            })
            dismiss()
        }
    }

    companion object {

        const val REQUEST_KEY = "LiveLocationLabsFlagPromotionBottomSheetRequest"
        const val BUNDLE_KEY_LABS_APPROVAL = "BUNDLE_KEY_LABS_APPROVAL"

        fun newInstance(): LiveLocationLabsFlagPromotionBottomSheet {
            return LiveLocationLabsFlagPromotionBottomSheet()
        }
    }
}
