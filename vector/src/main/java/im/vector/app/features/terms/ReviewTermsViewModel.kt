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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.terms.GetTermsResponse
import org.matrix.android.sdk.internal.util.awaitCallback
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class ReviewTermsViewModel @AssistedInject constructor(
        @Assisted initialState: ReviewTermsViewState,
        private val session: Session
) : VectorViewModel<ReviewTermsViewState, ReviewTermsAction, ReviewTermsViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: ReviewTermsViewState): ReviewTermsViewModel
    }

    companion object : MvRxViewModelFactory<ReviewTermsViewModel, ReviewTermsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ReviewTermsViewState): ReviewTermsViewModel? {
            val activity: ReviewTermsActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.viewModelFactory.create(state)
        }
    }

    lateinit var termsArgs: ServiceTermsArgs

    override fun handle(action: ReviewTermsAction) {
        when (action) {
            is ReviewTermsAction.LoadTerms          -> loadTerms(action)
            is ReviewTermsAction.MarkTermAsAccepted -> markTermAsAccepted(action)
            ReviewTermsAction.Accept                -> acceptTerms()
        }.exhaustive
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
                awaitCallback<Unit> {
                    session.agreeToTerms(
                            termsArgs.type,
                            termsArgs.baseURL,
                            agreedUrls,
                            termsArgs.token,
                            it
                    )
                }
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
                val data = awaitCallback<GetTermsResponse> {
                    session.getTerms(termsArgs.type, termsArgs.baseURL, it)
                }
                val terms = data.serverResponse.getLocalizedTerms(action.preferredLanguageCode).map {
                    Term(it.localizedUrl ?: "",
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
