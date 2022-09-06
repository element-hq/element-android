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

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentOtherSessionsBinding
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterBottomSheet

@AndroidEntryPoint
class OtherSessionsFragment : VectorBaseFragment<FragmentOtherSessionsBinding>(), VectorBaseBottomSheetDialogFragment.ResultListener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOtherSessionsBinding {
        return FragmentOtherSessionsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.otherSessionsToolbar).allowBack()
        initFilterView()
    }

    private fun initFilterView() {
        views.otherSessionsFilterFrameLayout.debouncedClicks {
            DeviceManagerFilterBottomSheet
                    .newInstance(this)
                    .show(requireActivity().supportFragmentManager, "SHOW_DEVICE_MANAGER_FILTER_BOTTOM_SHEET")
        }
    }

    override fun onBottomSheetResult(resultCode: Int, data: Any?) {
        if (resultCode == RESULT_OK && data != null) {
            Toast.makeText(requireContext(), data.toString(), Toast.LENGTH_LONG).show()
        }
    }
}
