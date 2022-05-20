/*
 * Copyright (c) 2020 New Vector Ltd
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
package im.vector.app.features.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentReviewTermsBinding
import org.matrix.android.sdk.api.session.terms.TermsService
import javax.inject.Inject

class ReviewTermsFragment @Inject constructor(
        private val termsController: TermsController
) : VectorBaseFragment<FragmentReviewTermsBinding>(),
        TermsController.Listener {

    private val reviewTermsViewModel: ReviewTermsViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentReviewTermsBinding {
        return FragmentReviewTermsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        termsController.description = when (reviewTermsViewModel.termsArgs.type) {
            TermsService.ServiceType.IdentityService    -> getString(R.string.terms_description_for_identity_server)
            TermsService.ServiceType.IntegrationManager -> getString(R.string.terms_description_for_integration_manager)
        }

        termsController.listener = this
        views.reviewTermsRecyclerView.configureWith(termsController)

        views.reviewTermsAccept.onClick { reviewTermsViewModel.handle(ReviewTermsAction.Accept) }
        views.reviewTermsDecline.onClick { activity?.finish() }

        reviewTermsViewModel.observeViewEvents {
            when (it) {
                is ReviewTermsViewEvents.Loading -> showLoading(it.message)
                is ReviewTermsViewEvents.Failure -> {
                    // Dialog is displayed by the Activity
                }
                ReviewTermsViewEvents.Success    -> {
                    // Handled by the Activity
                }
            }
        }

        reviewTermsViewModel.handle(ReviewTermsAction.LoadTerms(getString(R.string.resources_language)))
    }

    override fun onDestroyView() {
        views.reviewTermsRecyclerView.cleanup()
        termsController.listener = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.terms_of_service)
    }

    override fun invalidate() = withState(reviewTermsViewModel) { state ->
        termsController.setData(state)

        when (state.termsList) {
            is Loading -> {
                views.reviewTermsBottomBar.isVisible = false
            }
            is Success -> {
                views.reviewTermsBottomBar.isVisible = true
                views.reviewTermsAccept.isEnabled = state.termsList.invoke().all { it.accepted }
            }
            else       -> Unit
        }
    }

    override fun retry() {
        reviewTermsViewModel.handle(ReviewTermsAction.LoadTerms(getString(R.string.resources_language)))
    }

    override fun setChecked(term: Term, isChecked: Boolean) {
        reviewTermsViewModel.handle(ReviewTermsAction.MarkTermAsAccepted(term.url, isChecked))
    }

    override fun review(term: Term) {
        openUrlInChromeCustomTab(requireContext(), null, term.url)
    }
}
