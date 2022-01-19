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

package im.vector.app.features.home.room.detail.e2einfo

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class IncomingKeyRequestArgs(
        val requestId: String
) : Parcelable

class ReviewRequestFragment @Inject constructor(
        private val controller: RequestReviewController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(), RequestReviewController.Callback {

    val sharedViewModel: CryptoInfoViewModel by activityViewModel()
    private val requestViewModel by fragmentViewModel(RequestInfoViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentGenericRecyclerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.genericRecyclerView.configureWith(controller, disableItemAnimation = true)
        controller.callback = this

        requestViewModel.viewEvents.stream().onEach {
            when (it) {
                is RequestInfoEvent.DisplayConfirmAlert -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(
                                    if (it.isError) {
                                        R.string.dialog_title_error
                                    } else {
                                        R.string.dialog_title_success
                                    }
                            ))
                            .setMessage(it.message)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok) { _, _ ->
                            }
                            .show()
                }
            }
        }
    }

    override fun invalidate() = withState(requestViewModel) {
        controller.setData(it)

        when (it.sharingStatus) {
            is Loading -> {
                showWaitingView("Forwarding Room Key")
            }
            else       -> {
                hideWaitingView()
            }
        }
    }

    fun showWaitingView(text: String?) {
        views.waitingView.waitingView.isVisible = true
        views.waitingView.waitingCircularProgress.isVisible = true
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
        views.waitingView.waitingStatusText.setTextOrHide(text)
    }

    fun hideWaitingView() {
        views.waitingView.waitingView.isVisible = false
        views.waitingView.waitingStatusText.setTextOrHide(null)
        views.waitingView.waitingCircularProgress.isVisible = false
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
    }

    override fun doTryForward() {
        requestViewModel.handle(RequestInfoAction.TryToForward)
    }
}
