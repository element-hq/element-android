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

package im.vector.app.features.rageshake

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull

class BugReportViewModel @AssistedInject constructor(
        @Assisted initialState: BugReportState,
        val activeSessionHolder: ActiveSessionHolder
) : VectorViewModel<BugReportState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: BugReportState): BugReportViewModel
    }

    companion object : MavericksViewModelFactory<BugReportViewModel, BugReportState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: BugReportState): BugReportViewModel? {
            val activity: BugReportActivity = (viewModelContext as ActivityViewModelContext).activity()
            return activity.bugReportViewModelFactory.create(state)
        }
    }

    init {
        fetchHomeserverVersion()
    }

    private fun fetchHomeserverVersion() {
        viewModelScope.launch {
            val version = tryOrNull {
                activeSessionHolder.getSafeActiveSession()
                        ?.federationService()
                        ?.getFederationVersion()
                        ?.let { "${it.name} - ${it.version}" }
            } ?: "undefined"

            setState {
                copy(
                        serverVersion = version
                )
            }
        }
    }

    override fun handle(action: EmptyAction) {
        // No op
    }
}
