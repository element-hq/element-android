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

package im.vector.app.features.spaces

import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.args
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.checkedChanges
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.styleMatchingText
import im.vector.app.databinding.BottomSheetLeaveSpaceBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.parcelize.Parcelize
import me.gujun.android.span.span
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class LeaveSpaceBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetLeaveSpaceBinding>() {

    val settingsViewModel: SpaceMenuViewModel by parentFragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetLeaveSpaceBinding {
        return BottomSheetLeaveSpaceBinding.inflate(inflater, container, false)
    }

    @Inject lateinit var colorProvider: ColorProvider

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    @Parcelize
    data class Args(
            val spaceId: String
    ) : Parcelable

    override val showExpanded = true

    private val spaceArgs: SpaceBottomSheetSettingsArgs by args()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.autoLeaveRadioGroup.checkedChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        views.leaveAll.id      -> {
                            settingsViewModel.handle(SpaceMenuViewAction.SetAutoLeaveAll)
                        }
                        views.leaveNone.id     -> {
                            settingsViewModel.handle(SpaceMenuViewAction.SetAutoLeaveNone)
                        }
                        views.leaveSelected.id -> {
                            settingsViewModel.handle(SpaceMenuViewAction.SetAutoLeaveSelected)
                        }
                    }
                }
                .disposeOnDestroyView()

        views.leaveButton.debouncedClicks {
            settingsViewModel.handle(SpaceMenuViewAction.LeaveSpace)
        }

        views.cancelButton.debouncedClicks {
            dismiss()
        }
    }

    override fun invalidate() = withState(settingsViewModel) { state ->
        super.invalidate()

        val spaceSummary = state.spaceSummary ?: return@withState
        val bestName = spaceSummary.toMatrixItem().getBestName()
        val commonText = getString(R.string.space_leave_prompt_msg, bestName)
                .toSpannable().styleMatchingText(bestName, Typeface.BOLD)

        val warningMessage: CharSequence = if (spaceSummary.otherMemberIds.isEmpty()) {
            span {
                +commonText
                +"\n\n"
                span(getString(R.string.space_leave_prompt_msg_only_you)) {
                    textColor = colorProvider.getColorFromAttribute(R.attr.colorError)
                }
            }
        } else if (state.isLastAdmin) {
            span {
                +commonText
                +"\n\n"
                span(getString(R.string.space_leave_prompt_msg_as_admin)) {
                    textColor = colorProvider.getColorFromAttribute(R.attr.colorError)
                }
            }
        } else if (!spaceSummary.isPublic) {
            span {
                +commonText
                +"\n\n"
                span(getString(R.string.space_leave_prompt_msg_private)) {
                    textColor = colorProvider.getColorFromAttribute(R.attr.colorError)
                }
            }
        } else {
            commonText
        }

        views.bottomLeaveSpaceWarningText.setTextOrHide(warningMessage)

        if (state.leavingState is Loading) {
            views.leaveButton.isInvisible = true
            views.cancelButton.isInvisible = true
            views.leaveProgress.isVisible = true
        } else {
            views.leaveButton.isInvisible = false
            views.cancelButton.isInvisible = false
            views.leaveProgress.isVisible = false
        }

        when (state.leaveMode) {
            SpaceMenuState.LeaveMode.LEAVE_ALL      -> {
                views.autoLeaveRadioGroup.check(views.leaveAll.id)
            }
            SpaceMenuState.LeaveMode.LEAVE_NONE     -> {
                views.autoLeaveRadioGroup.check(views.leaveNone.id)
            }
            SpaceMenuState.LeaveMode.LEAVE_SELECTED -> {
                views.autoLeaveRadioGroup.check(views.leaveSelected.id)
            }
        }
    }

    companion object {

        fun newInstance(spaceId: String)
                : LeaveSpaceBottomSheet {
            return LeaveSpaceBottomSheet().apply {
                setArguments(SpaceBottomSheetSettingsArgs(spaceId))
            }
        }
    }
}
