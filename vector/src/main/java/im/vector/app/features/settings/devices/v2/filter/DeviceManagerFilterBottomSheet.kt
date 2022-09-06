/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.databinding.BottomSheetDeviceManagerFilterBinding
import im.vector.app.features.settings.devices.v2.list.SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS

@AndroidEntryPoint
class DeviceManagerFilterBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetDeviceManagerFilterBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetDeviceManagerFilterBinding {
        return BottomSheetDeviceManagerFilterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFilterRadioGroup()
    }

    private fun initFilterRadioGroup() {
        views.filterOptionInactiveRadioButton.text = resources.getQuantityString(
                R.plurals.device_manager_filter_option_inactive_description,
                SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
        )

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
        fun newInstance(resultListener: ResultListener): DeviceManagerFilterBottomSheet {
            val bottomSheet = DeviceManagerFilterBottomSheet()
            bottomSheet.resultListener = resultListener
            return bottomSheet
        }
    }
}
