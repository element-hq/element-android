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

package im.vector.app.features.widgets

import android.net.Uri
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.widgets.permissions.WidgetPermissionsHelper
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.api.session.widgets.WidgetManagementFailure
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.mapOptional
import org.matrix.android.sdk.flow.unwrap
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection

class WidgetViewModel @AssistedInject constructor(@Assisted val initialState: WidgetViewState,
                                                  widgetPostAPIHandlerFactory: WidgetPostAPIHandler.Factory,
                                                  private val stringProvider: StringProvider,
                                                  private val session: Session) :
    VectorViewModel<WidgetViewState, WidgetAction, WidgetViewEvents>(initialState),
        WidgetPostAPIHandler.NavigationCallback,
        IntegrationManagerService.Listener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<WidgetViewModel, WidgetViewState> {
        override fun create(initialState: WidgetViewState): WidgetViewModel
    }

    companion object : MavericksViewModelFactory<WidgetViewModel, WidgetViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)
    private val widgetService = session.widgetService()
    private val integrationManagerService = session.integrationManagerService()
    private val widgetURLFormatter = widgetService.getWidgetURLFormatter()
    private val postAPIMediator = widgetService.getWidgetPostAPIMediator()
    private var widgetPostAPIHandler: WidgetPostAPIHandler? = null

    // Flag to avoid infinite loop
    private var canRefreshToken = true

    init {
        integrationManagerService.addListener(this)
        if (initialState.widgetKind.isAdmin()) {
            widgetPostAPIHandler = widgetPostAPIHandlerFactory.create(initialState.roomId).apply {
                navigationCallback = this@WidgetViewModel
            }
            postAPIMediator.setHandler(widgetPostAPIHandler)
        }
        if (!integrationManagerService.isIntegrationEnabled()) {
            _viewEvents.post(WidgetViewEvents.Close(null))
        }
        setupName()
        refreshPermissionStatus()
        observePowerLevel()
        observeWidgetIfNeeded()
        subscribeToWidget()
    }

    private fun subscribeToWidget() {
        onAsync(WidgetViewState::asyncWidget) {
            setState { copy(widgetName = it.name) }
        }
    }

    private fun setupName() {
        val nameRes = initialState.widgetKind.nameRes
        if (nameRes != 0) {
            val name = stringProvider.getString(nameRes)
            setState { copy(widgetName = name) }
        }
    }

    private fun observePowerLevel() {
        if (room == null) {
            return
        }
        room.flow().liveStateEvent(EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<PowerLevelsContent>() }
                .unwrap()
                .map {
                    PowerLevelsHelper(it).isUserAllowedToSend(session.myUserId, true, null)
                }
                .setOnEach {
                    copy(canManageWidgets = it)
                }
    }

    private fun observeWidgetIfNeeded() {
        if (initialState.widgetKind != WidgetKind.ROOM) {
            return
        }
        val widgetId = initialState.widgetId ?: return
        session.flow()
                .liveRoomWidgets(initialState.roomId, QueryStringValue.Equals(widgetId))
                .filter { it.isNotEmpty() }
                .map { it.first() }
                .execute {
                    copy(asyncWidget = it)
                }
    }

    fun getPostAPIMediator() = postAPIMediator

    override fun handle(action: WidgetAction) {
        when (action) {
            is WidgetAction.OnWebViewLoadingError   -> handleWebViewLoadingError(action)
            is WidgetAction.OnWebViewLoadingSuccess -> handleWebViewLoadingSuccess(action)
            is WidgetAction.OnWebViewStartedToLoad  -> handleWebViewStartLoading()
            WidgetAction.LoadFormattedUrl           -> loadFormattedUrl(forceFetchToken = false)
            WidgetAction.DeleteWidget               -> handleDeleteWidget()
            WidgetAction.RevokeWidget               -> handleRevokeWidget()
            WidgetAction.OnTermsReviewed            -> loadFormattedUrl(forceFetchToken = false)
        }
    }

    private fun handleRevokeWidget() {
        viewModelScope.launch {
            val widgetId = initialState.widgetId ?: return@launch
            try {
                WidgetPermissionsHelper(integrationManagerService, widgetService).changePermission(initialState.roomId, widgetId, false)
                _viewEvents.post(WidgetViewEvents.Close())
            } catch (failure: Throwable) {
                _viewEvents.post(WidgetViewEvents.Failure(failure))
            }
        }
    }

    private fun handleDeleteWidget() {
        viewModelScope.launch {
            val widgetId = initialState.widgetId ?: return@launch
            try {
                widgetService.destroyRoomWidget(initialState.roomId, widgetId)
                _viewEvents.post(WidgetViewEvents.Close())
            } catch (failure: Throwable) {
                _viewEvents.post(WidgetViewEvents.Failure(failure))
            }
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
            val roomWidget = widgetService.getRoomWidgets(
                    roomId = initialState.roomId,
                    widgetId = QueryStringValue.Equals(widgetId, QueryStringValue.Case.SENSITIVE)
            ).firstOrNull()
            if (roomWidget == null) {
                setWidgetStatus(WidgetStatus.WIDGET_NOT_ALLOWED)
                return
            }
            if (roomWidget.event.senderId == session.myUserId) {
                setWidgetStatus(WidgetStatus.WIDGET_ALLOWED)
            } else {
                val stateEventId = roomWidget.event.eventId
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
                val formattedUrl = widgetURLFormatter.format(
                        baseUrl = initialState.baseUrl,
                        params = initialState.urlParams,
                        forceFetchScalarToken = forceFetchToken,
                        bypassWhitelist = initialState.widgetKind == WidgetKind.INTEGRATION_MANAGER
                )
                setState { copy(formattedURL = Success(formattedUrl)) }
                Timber.v("Post load formatted url event: $formattedUrl")
                _viewEvents.post(WidgetViewEvents.OnURLFormatted(formattedUrl))
            } catch (failure: Throwable) {
                if (failure is WidgetManagementFailure.TermsNotSignedException) {
                    // Terms for IM shouldn't have path appended
                    val displayTermsBaseUrl = Uri.parse(initialState.baseUrl).buildUpon().path("").toString()
                    _viewEvents.post(WidgetViewEvents.DisplayTerms(displayTermsBaseUrl, failure.token))
                }
                setState { copy(formattedURL = Fail(failure)) }
            }
        }
    }

    private fun handleWebViewStartLoading() {
        setState { copy(webviewLoadedUrl = Loading()) }
    }

    private fun handleWebViewLoadingSuccess(action: WidgetAction.OnWebViewLoadingSuccess) {
        if (initialState.widgetKind.isAdmin()) {
            postAPIMediator.injectAPI()
        }
        setState { copy(webviewLoadedUrl = Success(action.url)) }
    }

    private fun handleWebViewLoadingError(action: WidgetAction.OnWebViewLoadingError) = withState {
        if (!action.url.startsWith(it.baseUrl)) {
            return@withState
        }
        if (action.isHttpError) {
            // In case of 403, try to refresh the scalar token
            if (it.formattedURL is Success && action.errorCode == HttpsURLConnection.HTTP_FORBIDDEN && canRefreshToken) {
                canRefreshToken = false
                loadFormattedUrl(true)
            }
        } else {
            setState { copy(webviewLoadedUrl = Fail(Throwable(action.errorDescription))) }
        }
    }

    override fun onCleared() {
        integrationManagerService.removeListener(this)
        widgetPostAPIHandler?.navigationCallback = null
        postAPIMediator.setHandler(null)
        super.onCleared()
    }

    // IntegrationManagerService.Listener

    override fun onWidgetPermissionsChanged(widgets: Map<String, Boolean>) {
        refreshPermissionStatus()
    }

    override fun onIsEnabledChanged(enabled: Boolean) {
        if (!enabled) {
            _viewEvents.post(WidgetViewEvents.Close(null))
        }
    }

    // WidgetPostAPIHandler.NavigationCallback

    override fun close() {
        _viewEvents.post(WidgetViewEvents.Close(null))
    }

    override fun closeWithResult(content: Content) {
        _viewEvents.post(WidgetViewEvents.Close(content))
    }

    override fun openIntegrationManager(integId: String?, integType: String?) {
        _viewEvents.post(WidgetViewEvents.DisplayIntegrationManager(integId, integType))
    }
}
