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

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.args
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.styleMatchingText
import im.vector.app.databinding.BottomSheetLeaveSpaceBinding
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.spaces.leave.SpaceLeaveAdvancedActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import me.gujun.android.span.span
import org.matrix.android.sdk.api.util.toMatrixItem
import reactivecircus.flowbinding.android.widget.checkedChanges
import javax.inject.Inject

@AndroidEntryPoint
class LeaveSpaceBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetLeaveSpaceBinding>() {

    val settingsViewModel: SpaceMenuViewModel by parentFragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetLeaveSpaceBinding {
        return BottomSheetLeaveSpaceBinding.inflate(inflater, container, false)
    }

    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var errorFormatter: ErrorFormatter

    @Parcelize
    data class Args(
            val spaceId: String
    ) : Parcelable

    override val showExpanded = true

    private val spaceArgs: SpaceBottomSheetSettingsArgs by args()

    private val cherryPickLeaveActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            // nothing actually?
        } else {
            // move back to default
            settingsViewModel.handle(SpaceLeaveViewAction.SetAutoLeaveAll)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.autoLeaveRadioGroup.checkedChanges()
                .onEach {
                    when (it) {
                        views.leaveAll.id      -> {
                            settingsViewModel.handle(SpaceLeaveViewAction.SetAutoLeaveAll)
                        }
                        views.leaveNone.id     -> {
                            settingsViewModel.handle(SpaceLeaveViewAction.SetAutoLeaveNone)
                        }
                        views.leaveSelected.id -> {
                            settingsViewModel.handle(SpaceLeaveViewAction.SetAutoLeaveSelected)
                            // launch dedicated activity
                            cherryPickLeaveActivityResult.launch(
                                    SpaceLeaveAdvancedActivity.newIntent(requireContext(), spaceArgs.spaceId)
                            )
                        }
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.leaveButton.debouncedClicks {
            settingsViewModel.handle(SpaceLeaveViewAction.LeaveSpace)
        }

        views.cancelButton.debouncedClicks {
            dismiss()
        }
    }

    override fun invalidate() = withState(settingsViewModel) { state ->
        super.invalidate()

        val spaceSummary = state.spaceSummary ?: return@withState
        val bestName = spaceSummary.toMatrixItem().getBestName()
        val commonText = getString(R.string.space_leave_prompt_msg_with_name, bestName)
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

        views.inlineErrorText.setTextOrHide(null)
        if (state.leavingState is Loading) {
            views.leaveButton.isInvisible = true
            views.cancelButton.isInvisible = true
            views.leaveProgress.isVisible = true
        } else {
            views.leaveButton.isInvisible = false
            views.cancelButton.isInvisible = false
            views.leaveProgress.isVisible = false
            if (state.leavingState is Fail) {
                views.inlineErrorText.setTextOrHide(errorFormatter.toHumanReadable(state.leavingState.error))
            }
        }

        val hasChildren = (spaceSummary.spaceChildren?.size ?: 0) > 0
        if (hasChildren) {
            views.autoLeaveRadioGroup.isVisible = true
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
        } else {
            views.autoLeaveRadioGroup.isVisible = false
        }
    }

    companion object {

        fun newInstance(spaceId: String): LeaveSpaceBottomSheet {
            return LeaveSpaceBottomSheet().apply {
                setArguments(SpaceBottomSheetSettingsArgs(spaceId))
            }
        }
    }
}
