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
package im.vector.app.features.widgets.permissions

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import im.vector.app.R
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.net.URL

class RoomWidgetPermissionViewModel @AssistedInject constructor(@Assisted val initialState: RoomWidgetPermissionViewState,
                                                                private val session: Session)
    : VectorViewModel<RoomWidgetPermissionViewState, RoomWidgetPermissionActions, RoomWidgetPermissionViewEvents>(initialState) {

    private val widgetService = session.widgetService()
    private val integrationManagerService = session.integrationManagerService()

    init {
        observeWidget()
    }

    private fun observeWidget() {
        val widgetId = initialState.widgetId ?: return
        session.rx()
                .liveRoomWidgets(initialState.roomId, QueryStringValue.Equals(widgetId))
                .filter { it.isNotEmpty() }
                .map {
                    val widget = it.first()
                    val domain = try {
                        URL(widget.computedUrl).host
                    } catch (e: Throwable) {
                        null
                    }
                    // TODO check from widget urls the perms that should be shown?
                    // For now put all
                    if (widget.type == WidgetType.Jitsi) {
                        val infoShared = listOf(
                                R.string.room_widget_permission_display_name,
                                R.string.room_widget_permission_avatar_url
                        )
                        RoomWidgetPermissionViewState.WidgetPermissionData(
                                widget = widget,
                                isWebviewWidget = false,
                                permissionsList = infoShared,
                                widgetDomain = widget.widgetContent.data["domain"] as? String
                        )
                    } else {
                        val infoShared = listOf(
                                R.string.room_widget_permission_display_name,
                                R.string.room_widget_permission_avatar_url,
                                R.string.room_widget_permission_user_id,
                                R.string.room_widget_permission_theme,
                                R.string.room_widget_permission_widget_id,
                                R.string.room_widget_permission_room_id
                        )
                        RoomWidgetPermissionViewState.WidgetPermissionData(
                                widget = widget,
                                isWebviewWidget = true,
                                permissionsList = infoShared,
                                widgetDomain = domain
                        )
                    }
                }
                .execute {
                    copy(permissionData = it)
                }
    }

    override fun handle(action: RoomWidgetPermissionActions) {
        when (action) {
            RoomWidgetPermissionActions.AllowWidget -> handleAllowWidget()
            RoomWidgetPermissionActions.BlockWidget -> handleRevokeWidget()
        }
    }

    private fun handleRevokeWidget() = withState { state ->
        viewModelScope.launch {
            try {
                val widgetId = state.widgetId ?: return@launch
                if (state.permissionData()?.isWebviewWidget.orFalse()) {
                    WidgetPermissionsHelper(integrationManagerService, widgetService).changePermission(state.roomId, widgetId, false)
                } else {
                    awaitCallback<Unit> {
                        session.integrationManagerService().setNativeWidgetDomainAllowed(
                                state.permissionData.invoke()?.widget?.type?.preferred ?: "",
                                state.permissionData.invoke()?.widgetDomain ?: "",
                                false,
                                it
                        )
                    }
                }
            } catch (failure: Throwable) {
                Timber.v("Failure revoking widget: ${state.widgetId}")
            } finally {
                // We send close event in every situation
                _viewEvents.post(RoomWidgetPermissionViewEvents.Close)
            }
        }
    }

    private fun handleAllowWidget() = withState { state ->
        viewModelScope.launch {
            try {
                val widgetId = state.widgetId ?: return@launch
                if (state.permissionData()?.isWebviewWidget.orFalse()) {
                    WidgetPermissionsHelper(integrationManagerService, widgetService).changePermission(state.roomId, widgetId, true)
                } else {
                    awaitCallback<Unit> {
                        session.integrationManagerService().setNativeWidgetDomainAllowed(
                                state.permissionData.invoke()?.widget?.type?.preferred ?: "",
                                state.permissionData.invoke()?.widgetDomain ?: "",
                                true,
                                it
                        )
                    }
                }
            } catch (failure: Throwable) {
                Timber.v("Failure allowing widget: ${state.widgetId}")
                // We send close event only when it's failed
                _viewEvents.post(RoomWidgetPermissionViewEvents.Close)
            }
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: RoomWidgetPermissionViewState): RoomWidgetPermissionViewModel
    }

    companion object : MvRxViewModelFactory<RoomWidgetPermissionViewModel, RoomWidgetPermissionViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomWidgetPermissionViewState): RoomWidgetPermissionViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }
}
