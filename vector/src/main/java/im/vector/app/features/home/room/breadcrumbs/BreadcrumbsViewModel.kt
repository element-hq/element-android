/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.breadcrumbs

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import io.reactivex.schedulers.Schedulers
import org.matrix.android.sdk.rx.rx

class BreadcrumbsViewModel @AssistedInject constructor(@Assisted initialState: BreadcrumbsViewState,
                                                       private val session: Session)
    : VectorViewModel<BreadcrumbsViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: BreadcrumbsViewState): BreadcrumbsViewModel
    }

    companion object : MvRxViewModelFactory<BreadcrumbsViewModel, BreadcrumbsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: BreadcrumbsViewState): BreadcrumbsViewModel? {
            val fragment: BreadcrumbsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.breadcrumbsViewModelFactory.create(state)
        }
    }

    init {
        observeBreadcrumbs()
    }

    override fun handle(action: EmptyAction) {
        // No op
    }

    // PRIVATE METHODS *****************************************************************************

    private fun observeBreadcrumbs() {
        session.rx()
                .liveBreadcrumbs(roomSummaryQueryParams {
                    displayName = QueryStringValue.NoCondition
                    memberships = listOf(Membership.JOIN)
                })
                .observeOn(Schedulers.computation())
                .execute { asyncBreadcrumbs ->
                    copy(asyncBreadcrumbs = asyncBreadcrumbs)
                }
    }
}
