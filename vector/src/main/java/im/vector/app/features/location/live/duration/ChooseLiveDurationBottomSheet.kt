/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.duration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.databinding.BottomSheetChooseLiveLocationShareDurationBinding

/**
 * 15 minutes.
 */
private const val DURATION_IN_MS_OPTION_1 = 15 * 60_000L

/**
 * 1 hour.
 */
private const val DURATION_IN_MS_OPTION_2 = 60 * 60_000L

/**
 * 8 hours.
 */
private const val DURATION_IN_MS_OPTION_3 = 8 * 60 * 60_000L

/**
 * Bottom sheet displaying list of options to choose the duration of the location live sharing.
 */
@AndroidEntryPoint
class ChooseLiveDurationBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetChooseLiveLocationShareDurationBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetChooseLiveLocationShareDurationBinding {
        return BottomSheetChooseLiveLocationShareDurationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initConfirmButton()
    }

    // we are not using state for this one as it's static, so no need to override invalidate()

    private fun initConfirmButton() {
        views.liveLocShareChooseDurationConfirm.setOnClickListener {
            val currentChoice = getCurrentChoice()
            resultListener?.onBottomSheetResult(RESULT_OK, currentChoice)
            dismiss()
        }
    }

    private fun getCurrentChoice(): Long {
        return when (views.liveLocShareChooseDurationOptions.checkedRadioButtonId) {
            R.id.liveLocShareChooseDurationOption1 -> DURATION_IN_MS_OPTION_1
            R.id.liveLocShareChooseDurationOption2 -> DURATION_IN_MS_OPTION_2
            R.id.liveLocShareChooseDurationOption3 -> DURATION_IN_MS_OPTION_3
            else -> DURATION_IN_MS_OPTION_1
        }
    }

    companion object {
        fun newInstance(resultListener: ResultListener): ChooseLiveDurationBottomSheet {
            val bottomSheet = ChooseLiveDurationBottomSheet()
            bottomSheet.resultListener = resultListener
            return bottomSheet
        }
    }
}
