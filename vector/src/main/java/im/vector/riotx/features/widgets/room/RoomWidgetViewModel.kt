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

package im.vector.riotx.features.widgets.room

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.core.platform.VectorViewModel
import kotlinx.coroutines.launch

class RoomWidgetViewModel @AssistedInject constructor(@Assisted val initialState: WidgetViewState,
                                                      private val session: Session)
    : VectorViewModel<WidgetViewState, RoomWidgetAction, RoomWidgetViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: WidgetViewState): RoomWidgetViewModel
    }

    companion object : MvRxViewModelFactory<RoomWidgetViewModel, WidgetViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: WidgetViewState): RoomWidgetViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    private val widgetService = session.widgetService()
    private val integrationManagerService = session.integrationManagerService()
    private val widgetBuilder = widgetService.getWidgetURLBuilder()
    private val postAPIMediator = widgetService.getWidgetPostAPIMediator()

    init {
        refreshPermissionStatus()
    }

    fun getPostAPIMediator() = postAPIMediator

    override fun handle(action: RoomWidgetAction) {
        when (action) {
            is RoomWidgetAction.OnWebViewLoadingError   -> handleWebViewLoadingError(action.url)
            is RoomWidgetAction.OnWebViewLoadingSuccess -> handleWebViewLoadingSuccess(action.url)
            is RoomWidgetAction.OnWebViewStartedToLoad  -> handleWebViewStartLoading(action.url)
        }
    }

    private fun refreshPermissionStatus() {
        if (initialState.widgetKind == WidgetKind.USER || initialState.widgetKind == WidgetKind.INTEGRATION_MANAGER) {
            onWidgetAllowed()
        } else {
            val widgetId = initialState.widgetId
            if (widgetId == null) {
                setState { copy(status = WidgetStatus.WIDGET_NOT_ALLOWED) }
                return
            }
            val roomWidget = widgetService.getRoomWidgets(initialState.roomId, widgetId = QueryStringValue.Equals(widgetId, QueryStringValue.Case.SENSITIVE)).firstOrNull()
            if (roomWidget == null) {
                setState { copy(status = WidgetStatus.WIDGET_NOT_ALLOWED) }
                return
            }
            if (roomWidget.event?.senderId == session.myUserId) {
                onWidgetAllowed()
            } else {
                val stateEventId = roomWidget.event?.eventId
                // This should not happen
                if (stateEventId == null) {
                    setState { copy(status = WidgetStatus.WIDGET_NOT_ALLOWED) }
                    return
                }
                val isAllowed = integrationManagerService.isWidgetAllowed(stateEventId)
                if (!isAllowed) {
                    setState { copy(status = WidgetStatus.WIDGET_NOT_ALLOWED) }
                } else {
                    onWidgetAllowed()
                }
            }
        }
    }

    private fun onWidgetAllowed() {
        setState {
            copy(status = WidgetStatus.WIDGET_ALLOWED, formattedURL = Loading())
        }
        viewModelScope.launch {
            try {
                val formattedUrl = widgetBuilder.build(initialState.baseUrl)
                setState { copy(formattedURL = Success(formattedUrl)) }
            } catch (failure: Throwable) {
                setState { copy(formattedURL = Fail(failure)) }
            }
        }
    }

    private fun handleWebViewStartLoading(url: String) {
    }

    private fun handleWebViewLoadingSuccess(url: String) {
    }

    private fun handleWebViewLoadingError(url: String) {
    }
}
