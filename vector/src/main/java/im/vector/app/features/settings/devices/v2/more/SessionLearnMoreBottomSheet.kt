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

package im.vector.app.features.settings.devices.v2.more

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetSessionLearnMoreBinding
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class SessionLearnMoreBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSessionLearnMoreBinding>() {

    @Parcelize
    data class Args(
            val title: String,
            val description: String,
    ) : Parcelable

    private val viewModel: SessionLearnMoreViewModel by fragmentViewModel()

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSessionLearnMoreBinding {
        return BottomSheetSessionLearnMoreBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCloseButton()
    }

    private fun initCloseButton() {
        views.bottomSheetSessionLearnMoreCloseButton.debouncedClicks {
            dismiss()
        }
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        super.invalidate()
        views.bottomSheetSessionLearnMoreTitle.text = viewState.title
        views.bottomSheetSessionLearnMoreDescription.text = viewState.description
    }

    companion object {

        fun show(fragmentManager: FragmentManager, args: Args) {
            val bottomSheet = SessionLearnMoreBottomSheet()
            bottomSheet.isCancelable = true
            bottomSheet.setArguments(args)
            bottomSheet.show(fragmentManager, "SessionLearnMoreBottomSheet")
        }
    }
}
