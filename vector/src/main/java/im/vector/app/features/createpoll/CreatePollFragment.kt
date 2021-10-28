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

package im.vector.app.features.createpoll

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.withState
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentCreatePollBinding
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class CreatePollArgs(
        val roomId: String
) : Parcelable

class CreatePollFragment @Inject constructor(
        private val controller: CreatePollController,
        val createPollViewModelFactory: CreatePollViewModel.Factory
) : VectorBaseFragment<FragmentCreatePollBinding>(), CreatePollController.Callback {

    private val viewModel: CreatePollViewModel by activityViewModel()
    private val createPollArgs: CreatePollArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreatePollBinding {
        return FragmentCreatePollBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vectorBaseActivity.setSupportActionBar(views.createPollToolbar)

        views.createPollRecyclerView.configureWith(controller)
        controller.callback = this

        views.createPollClose.debouncedClicks {
            requireActivity().finish()
        }

        views.createPollButton.debouncedClicks {
            viewModel.handle(CreatePollAction.OnCreatePoll)
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
    }
}
