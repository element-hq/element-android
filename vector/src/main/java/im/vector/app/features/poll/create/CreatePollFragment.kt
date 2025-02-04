/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll.create

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentCreatePollBinding
import im.vector.app.features.poll.PollMode
import im.vector.app.features.poll.create.CreatePollViewModel.Companion.MAX_OPTIONS_COUNT
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.model.message.PollType
import javax.inject.Inject

@Parcelize
data class CreatePollArgs(
        val roomId: String,
        val editedEventId: String?,
        val mode: PollMode
) : Parcelable

@AndroidEntryPoint
class CreatePollFragment :
        VectorBaseFragment<FragmentCreatePollBinding>(),
        CreatePollController.Callback {

    @Inject lateinit var controller: CreatePollController

    private val viewModel: CreatePollViewModel by activityViewModel()
    private val args: CreatePollArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreatePollBinding {
        return FragmentCreatePollBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.createPollToolbar)
                .allowBack(useCross = true)

        when (args.mode) {
            PollMode.CREATE -> {
                views.createPollToolbar.title = getString(CommonStrings.create_poll_title)
                views.createPollButton.text = getString(CommonStrings.create_poll_title)
            }
            PollMode.EDIT -> {
                views.createPollToolbar.title = getString(CommonStrings.edit_poll_title)
                views.createPollButton.text = getString(CommonStrings.edit_poll_title)
            }
        }

        views.createPollRecyclerView.configureWith(controller, disableItemAnimation = true)
        // workaround for https://github.com/element-hq/element-android/issues/4735
        views.createPollRecyclerView.setItemViewCacheSize(MAX_OPTIONS_COUNT + 6)
        controller.callback = this

        views.createPollButton.debouncedClicks {
            viewModel.handle(CreatePollAction.OnCreatePoll)
        }

        viewModel.onEach(CreatePollViewState::canCreatePoll) { canCreatePoll ->
            views.createPollButton.isEnabled = canCreatePoll
        }

        viewModel.observeViewEvents {
            when (it) {
                CreatePollViewEvents.Success -> handleSuccess()
                CreatePollViewEvents.EmptyQuestionError -> handleEmptyQuestionError()
                is CreatePollViewEvents.NotEnoughOptionsError -> handleNotEnoughOptionsError(it.requiredOptionsCount)
            }
        }
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
    }

    override fun onQuestionChanged(question: String) {
        viewModel.handle(CreatePollAction.OnQuestionChanged(question))
    }

    override fun onOptionChanged(index: Int, option: String) {
        viewModel.handle(CreatePollAction.OnOptionChanged(index, option))
    }

    override fun onDeleteOption(index: Int) {
        viewModel.handle(CreatePollAction.OnDeleteOption(index))
    }

    override fun onAddOption() {
        viewModel.handle(CreatePollAction.OnAddOption)
        // Scroll to bottom to show "Add Option" button
        views.createPollRecyclerView.apply {
            postDelayed({
                smoothScrollToPosition(adapter?.itemCount?.minus(1) ?: 0)
            }, 100)
        }
    }

    override fun onPollTypeChanged(type: PollType) {
        viewModel.handle(CreatePollAction.OnPollTypeChanged(type))
    }

    private fun handleSuccess() {
        requireActivity().finish()
    }

    private fun handleEmptyQuestionError() {
        renderToast(getString(CommonStrings.create_poll_empty_question_error))
    }

    private fun handleNotEnoughOptionsError(requiredOptionsCount: Int) {
        renderToast(
                resources.getQuantityString(
                        CommonPlurals.create_poll_not_enough_options_error,
                        requiredOptionsCount,
                        requiredOptionsCount
                )
        )
    }

    private fun renderToast(message: String) {
        views.createPollToast.removeCallbacks(hideToastRunnable)
        views.createPollToast.text = message
        views.createPollToast.isVisible = true
        views.createPollToast.postDelayed(hideToastRunnable, 2_000)
    }

    private val hideToastRunnable = Runnable {
        views.createPollToast.isVisible = false
    }
}
