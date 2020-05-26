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
import im.vector.matrix.android.internal.session.widgets.WidgetManagementFailure
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.widgets.WidgetPostAPIHandler
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection

class RoomWidgetViewModel @AssistedInject constructor(@Assisted val initialState: WidgetViewState,
                                                      private val widgetPostAPIHandlerFactory: WidgetPostAPIHandler.Factory,
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
    private val widgetBuilder = widgetService.getWidgetURLFormatter()
    private val postAPIMediator = widgetService.getWidgetPostAPIMediator()

    init {
        if(initialState.widgetKind.isAdmin()) {
            val widgetPostAPIHandler = widgetPostAPIHandlerFactory.create(initialState.roomId)
            postAPIMediator.setHandler(widgetPostAPIHandler)
        }
        refreshPermissionStatus()
        observePermissionStatus()
    }

    private fun observePermissionStatus() {
        selectSubscribe(WidgetViewState::status) {
            Timber.v("Widget status: $it")
            if (it == WidgetStatus.WIDGET_ALLOWED) {
                loadFormattedUrl()
            }
        }
    }

    fun getPostAPIMediator() = postAPIMediator

    override fun handle(action: RoomWidgetAction) {
        when (action) {
            is RoomWidgetAction.OnWebViewLoadingError   -> handleWebViewLoadingError(action.isHttpError, action.errorCode, action.errorDescription)
            is RoomWidgetAction.OnWebViewLoadingSuccess -> handleWebViewLoadingSuccess(action.url)
            is RoomWidgetAction.OnWebViewStartedToLoad  -> handleWebViewStartLoading()
        }
    }

    private fun refreshPermissionStatus() {
        if (initialState.widgetKind.isAdmin()) {
            setWidgetStatus(WidgetStatus.WIDGET_ALLOWED)
        } else {
            val widgetId = initialState.widgetId
            if (widgetId == null) {
                setWidgetStatus(WidgetStatus.WIDGET_NOT_ALLOWED)
                return
            }
            val roomWidget = widgetService.getRoomWidgets(initialState.roomId, widgetId = QueryStringValue.Equals(widgetId, QueryStringValue.Case.SENSITIVE)).firstOrNull()
            if (roomWidget == null) {
                setWidgetStatus(WidgetStatus.WIDGET_NOT_ALLOWED)
                return
            }
            if (roomWidget.event?.senderId == session.myUserId) {
                setWidgetStatus(WidgetStatus.WIDGET_ALLOWED)
            } else {
                val stateEventId = roomWidget.event?.eventId
                // This should not happen
                if (stateEventId == null) {
                    setWidgetStatus(WidgetStatus.WIDGET_NOT_ALLOWED)
                    return
                }
                val isAllowed = integrationManagerService.isWidgetAllowed(stateEventId)
                if (!isAllowed) {
                    setWidgetStatus(WidgetStatus.WIDGET_NOT_ALLOWED)
                } else {
                    setWidgetStatus(WidgetStatus.WIDGET_ALLOWED)
                }
            }
        }
    }

    private fun setWidgetStatus(widgetStatus: WidgetStatus) {
        setState { copy(status = widgetStatus) }
    }

    private fun loadFormattedUrl(forceFetchToken: Boolean = false) {
        viewModelScope.launch {
            try {
                setState { copy(formattedURL = Loading()) }
                val formattedUrl = widgetBuilder.format(
                        baseUrl = initialState.baseUrl,
                        params = initialState.urlParams,
                        forceFetchScalarToken = forceFetchToken,
                        bypassWhitelist = initialState.widgetKind == WidgetKind.INTEGRATION_MANAGER
                )
                setState { copy(formattedURL = Success(formattedUrl)) }
                _viewEvents.post(RoomWidgetViewEvents.LoadFormattedURL(formattedUrl))
            } catch (failure: Throwable) {
                if (failure is WidgetManagementFailure.TermsNotSignedException) {
                    _viewEvents.post(RoomWidgetViewEvents.DisplayTerms(failure.baseUrl, failure.token))
                }
                setState { copy(formattedURL = Fail(failure)) }
            }
        }
    }

    private fun handleWebViewStartLoading() {
        setState { copy(webviewLoadedUrl = Loading()) }
    }

    private fun handleWebViewLoadingSuccess(url: String) {
        if (initialState.widgetKind.isAdmin()) {
            postAPIMediator.injectAPI()
        }
        setState { copy(webviewLoadedUrl = Success(url)) }
    }

    private fun handleWebViewLoadingError(isHttpError: Boolean, reason: Int, errorDescription: String) {
        if (isHttpError) {
            // In case of 403, try to refresh the scalar token
            if (reason == HttpsURLConnection.HTTP_FORBIDDEN) {
                loadFormattedUrl(true)
            }
        } else {
            setState { copy(webviewLoadedUrl = Fail(Throwable(errorDescription))) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        postAPIMediator.setHandler(null)
    }
}
