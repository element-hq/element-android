/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.widgets.permissions

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import java.net.URL

class RoomWidgetPermissionViewModel @AssistedInject constructor(
        @Assisted val initialState: RoomWidgetPermissionViewState,
        private val session: Session
) :
        VectorViewModel<RoomWidgetPermissionViewState, RoomWidgetPermissionActions, RoomWidgetPermissionViewEvents>(initialState) {

    private val widgetService = session.widgetService()
    private val integrationManagerService = session.integrationManagerService()

    init {
        observeWidget()
    }

    private fun observeWidget() {
        val widgetId = initialState.widgetId ?: return
        session.flow()
                .liveRoomWidgets(initialState.roomId, QueryStringValue.Equals(widgetId))
                .filter { it.isNotEmpty() }
                .map {
                    val widget = it.first()
                    val domain = tryOrNull { URL(widget.widgetContent.url) }?.host
                    // TODO check from widget urls the perms that should be shown?
                    // For now put all
                    if (widget.type == WidgetType.Jitsi) {
                        val infoShared = listOf(
                                CommonStrings.room_widget_permission_display_name,
                                CommonStrings.room_widget_permission_avatar_url
                        )
                        RoomWidgetPermissionViewState.WidgetPermissionData(
                                widget = widget,
                                isWebviewWidget = false,
                                permissionsList = infoShared,
                                widgetDomain = widget.widgetContent.data["domain"] as? String
                        )
                    } else {
                        val infoShared = listOf(
                                CommonStrings.room_widget_permission_display_name,
                                CommonStrings.room_widget_permission_avatar_url,
                                CommonStrings.room_widget_permission_user_id,
                                CommonStrings.room_widget_permission_theme,
                                CommonStrings.room_widget_permission_widget_id,
                                CommonStrings.room_widget_permission_room_id
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
                    session.integrationManagerService().setNativeWidgetDomainAllowed(
                            state.permissionData.invoke()?.widget?.type?.preferred ?: "",
                            state.permissionData.invoke()?.widgetDomain ?: "",
                            false
                    )
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
                    session.integrationManagerService().setNativeWidgetDomainAllowed(
                            state.permissionData.invoke()?.widget?.type?.preferred ?: "",
                            state.permissionData.invoke()?.widgetDomain ?: "",
                            true
                    )
                }
            } catch (failure: Throwable) {
                Timber.v("Failure allowing widget: ${state.widgetId}")
                // We send close event only when it's failed
                _viewEvents.post(RoomWidgetPermissionViewEvents.Close)
            }
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomWidgetPermissionViewModel, RoomWidgetPermissionViewState> {
        override fun create(initialState: RoomWidgetPermissionViewState): RoomWidgetPermissionViewModel
    }

    companion object : MavericksViewModelFactory<RoomWidgetPermissionViewModel, RoomWidgetPermissionViewState> by hiltMavericksViewModelFactory()
}
