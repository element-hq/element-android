/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.filter

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.databinding.BottomSheetDeviceManagerFilterBinding
import im.vector.app.features.settings.devices.v2.list.SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
import im.vector.lib.strings.CommonPlurals
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceManagerFilterBottomSheetArgs(
        val initialFilterType: DeviceManagerFilterType,
) : Parcelable

@AndroidEntryPoint
class DeviceManagerFilterBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetDeviceManagerFilterBinding>() {

    private val args: DeviceManagerFilterBottomSheetArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetDeviceManagerFilterBinding {
        return BottomSheetDeviceManagerFilterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFilterRadioGroup()
    }

    private fun initFilterRadioGroup() {
        views.filterOptionInactiveTextView.text = resources.getQuantityString(
                CommonPlurals.device_manager_filter_option_inactive_description,
                SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
        )

        val radioButtonId = when (args.initialFilterType) {
            DeviceManagerFilterType.ALL_SESSIONS -> R.id.filterOptionAllSessionsRadioButton
            DeviceManagerFilterType.VERIFIED -> R.id.filterOptionVerifiedRadioButton
            DeviceManagerFilterType.UNVERIFIED -> R.id.filterOptionUnverifiedRadioButton
            DeviceManagerFilterType.INACTIVE -> R.id.filterOptionInactiveRadioButton
        }
        views.filterOptionsRadioGroup.check(radioButtonId)

        views.filterOptionVerifiedTextView.debouncedClicks {
            views.filterOptionsRadioGroup.check(R.id.filterOptionVerifiedRadioButton)
        }
        views.filterOptionUnverifiedTextView.debouncedClicks {
            views.filterOptionsRadioGroup.check(R.id.filterOptionUnverifiedRadioButton)
        }
        views.filterOptionInactiveTextView.debouncedClicks {
            views.filterOptionsRadioGroup.check(R.id.filterOptionInactiveRadioButton)
        }

        views.filterOptionsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            onFilterTypeChanged(checkedId)
        }
    }

    private fun onFilterTypeChanged(checkedId: Int) {
        val filterType = when (checkedId) {
            R.id.filterOptionAllSessionsRadioButton -> DeviceManagerFilterType.ALL_SESSIONS
            R.id.filterOptionVerifiedRadioButton -> DeviceManagerFilterType.VERIFIED
            R.id.filterOptionUnverifiedRadioButton -> DeviceManagerFilterType.UNVERIFIED
            R.id.filterOptionInactiveRadioButton -> DeviceManagerFilterType.INACTIVE
            else -> DeviceManagerFilterType.ALL_SESSIONS
        }
        resultListener?.onBottomSheetResult(RESULT_OK, filterType)
        dismiss()
    }

    companion object {
        fun newInstance(initialFilterType: DeviceManagerFilterType, resultListener: ResultListener): DeviceManagerFilterBottomSheet {
            return DeviceManagerFilterBottomSheet().apply {
                this.resultListener = resultListener
                setArguments(DeviceManagerFilterBottomSheetArgs(initialFilterType))
            }
        }
    }
}
