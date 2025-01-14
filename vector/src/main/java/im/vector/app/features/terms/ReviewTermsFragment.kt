/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentReviewTermsBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.terms.TermsService
import javax.inject.Inject

@AndroidEntryPoint
class ReviewTermsFragment :
        VectorBaseFragment<FragmentReviewTermsBinding>(),
        TermsController.Listener {

    @Inject lateinit var termsController: TermsController

    private val reviewTermsViewModel: ReviewTermsViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentReviewTermsBinding {
        return FragmentReviewTermsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        termsController.description = when (reviewTermsViewModel.termsArgs.type) {
            TermsService.ServiceType.IdentityService -> getString(CommonStrings.terms_description_for_identity_server)
            TermsService.ServiceType.IntegrationManager -> getString(CommonStrings.terms_description_for_integration_manager)
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
                ReviewTermsViewEvents.Success -> {
                    // Handled by the Activity
                }
            }
        }

        reviewTermsViewModel.handle(ReviewTermsAction.LoadTerms(getString(CommonStrings.resources_language)))
    }

    override fun onDestroyView() {
        views.reviewTermsRecyclerView.cleanup()
        termsController.listener = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.terms_of_service)
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
            else -> Unit
        }
    }

    override fun retry() {
        reviewTermsViewModel.handle(ReviewTermsAction.LoadTerms(getString(CommonStrings.resources_language)))
    }

    override fun setChecked(term: Term, isChecked: Boolean) {
        reviewTermsViewModel.handle(ReviewTermsAction.MarkTermAsAccepted(term.url, isChecked))
    }

    override fun review(term: Term) {
        openUrlInChromeCustomTab(requireContext(), null, term.url)
    }
}
