/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.terms

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber

class ReviewTermsViewModel @AssistedInject constructor(
        @Assisted initialState: ReviewTermsViewState,
        private val session: Session
) : VectorViewModel<ReviewTermsViewState, ReviewTermsAction, ReviewTermsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ReviewTermsViewModel, ReviewTermsViewState> {
        override fun create(initialState: ReviewTermsViewState): ReviewTermsViewModel
    }

    companion object : MavericksViewModelFactory<ReviewTermsViewModel, ReviewTermsViewState> by hiltMavericksViewModelFactory()

    lateinit var termsArgs: ServiceTermsArgs

    override fun handle(action: ReviewTermsAction) {
        when (action) {
            is ReviewTermsAction.LoadTerms -> loadTerms(action)
            is ReviewTermsAction.MarkTermAsAccepted -> markTermAsAccepted(action)
            ReviewTermsAction.Accept -> acceptTerms()
        }
    }

    private fun markTermAsAccepted(action: ReviewTermsAction.MarkTermAsAccepted) = withState { state ->
        val newList = state.termsList.invoke()?.map {
            if (it.url == action.url) {
                it.copy(accepted = action.accepted)
            } else {
                it
            }
        }

        if (newList != null) {
            setState {
                state.copy(
                        termsList = Success(newList)
                )
            }
        }
    }

    private fun acceptTerms() = withState { state ->
        val acceptedTerms = state.termsList.invoke() ?: return@withState

        if (acceptedTerms.any { it.accepted.not() }) {
            // Should not happen
            _viewEvents.post(ReviewTermsViewEvents.Failure(IllegalStateException("Please accept all terms"), false))
            return@withState
        }

        _viewEvents.post(ReviewTermsViewEvents.Loading())

        val agreedUrls = acceptedTerms.map { it.url }

        viewModelScope.launch {
            try {
                session.termsService().agreeToTerms(
                        termsArgs.type,
                        termsArgs.baseURL,
                        agreedUrls,
                        termsArgs.token
                )
                _viewEvents.post(ReviewTermsViewEvents.Success)
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to agree to terms")
                _viewEvents.post(ReviewTermsViewEvents.Failure(failure, false))
            }
        }
    }

    private fun loadTerms(action: ReviewTermsAction.LoadTerms) = withState { state ->
        if (state.termsList is Loading || state.termsList is Success) {
            return@withState
        }

        setState {
            copy(termsList = Loading())
        }

        viewModelScope.launch {
            try {
                val data = session.termsService().getTerms(termsArgs.type, termsArgs.baseURL)
                val terms = data.serverResponse.getLocalizedTerms(action.preferredLanguageCode).map {
                    Term(
                            it.localizedUrl ?: "",
                            it.localizedName ?: "",
                            it.version,
                            accepted = data.alreadyAcceptedTermUrls.contains(it.localizedUrl)
                    )
                }

                setState {
                    copy(
                            termsList = Success(terms)
                    )
                }
            } catch (failure: Throwable) {
                Timber.e(failure, "Failed to load terms")
                setState {
                    copy(
                            termsList = Uninitialized
                    )
                }
                _viewEvents.post(ReviewTermsViewEvents.Failure(failure, true))
            }
        }
    }
}
