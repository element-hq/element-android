/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.threads.list.views

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.airbnb.mvrx.parentFragmentViewModel
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetThreadListBinding
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewModel
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewState
import im.vector.app.features.themes.ThemeUtils

class ThreadListBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetThreadListBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetThreadListBinding {
        return BottomSheetThreadListBinding.inflate(inflater, container, false)
    }

    private val threadListViewModel: ThreadListViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        threadListViewModel.onEach {
            renderState(it)
        }
        views.threadListModalAllThreads.views.bottomSheetActionClickableZone.debouncedClicks {
            threadListViewModel.applyFiltering(false)
            dismiss()
        }
        views.threadListModalMyThreads.views.bottomSheetActionClickableZone.debouncedClicks {
            threadListViewModel.applyFiltering(true)
            dismiss()
        }
    }

    private fun renderState(state: ThreadListViewState) {
        val radioOffDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_radio_off)
        val radioOnDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_radio_on)

        if (state.shouldFilterThreads) {
            setRightIconDrawableAllThreads(radioOffDrawable, R.attr.vctr_content_secondary)
            setRightIconDrawableMyThreads(radioOnDrawable, R.attr.colorPrimary)
        } else {
            setRightIconDrawableAllThreads(radioOnDrawable, R.attr.colorPrimary)
            setRightIconDrawableMyThreads(radioOffDrawable, R.attr.vctr_content_secondary)
        }
    }

    private fun setRightIconDrawableAllThreads(drawable: Drawable?, @AttrRes tint: Int) {
        views.threadListModalAllThreads.rightIcon = drawable
        views.threadListModalAllThreads.rightIcon?.setTintFromAttribute(tint)
    }

    private fun setRightIconDrawableMyThreads(drawable: Drawable?, @AttrRes tint: Int) {
        views.threadListModalMyThreads.rightIcon = drawable
        views.threadListModalMyThreads.rightIcon?.setTintFromAttribute(tint)
    }

    private fun Drawable.setTintFromAttribute(@AttrRes tint: Int) {
        DrawableCompat.setTint(this, ThemeUtils.getColor(requireContext(), tint))
    }
}
