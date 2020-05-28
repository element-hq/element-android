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
package im.vector.riotx.features.widgets.permissions

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.extensions.orFalse
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.rx.rx
import im.vector.riotx.R
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import java.net.URL

class RoomWidgetPermissionViewModel @AssistedInject constructor(@Assisted val initialState: RoomWidgetPermissionViewState,
                                                                private val session: Session)
    : VectorViewModel<RoomWidgetPermissionViewState, RoomWidgetPermissionActions, EmptyViewEvents>(initialState) {

    private val widgetService = session.widgetService()
    private val integrationManagerService = session.integrationManagerService()

    init {
        observeWidget()
    }

    private fun observeWidget() {
        val widgetId = initialState.widgetId
        session.rx()
                .liveRoomWidgets(initialState.roomId, QueryStringValue.Equals(widgetId))
                .filter { it.isNotEmpty() }
                .map {
                    val widget = it.first()
                    val domain = try {
                        URL(widget.widgetContent.url).host
                    } catch (e: Throwable) {
                        null
                    }
                    //TODO check from widget urls the perms that should be shown?
                    //For now put all
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
            if (state.permissionData()?.isWebviewWidget.orFalse()) {
                WidgetPermissionsHelper(integrationManagerService, widgetService).changePermission(state.roomId, state.widgetId, false)
            } else {
                //TODO JITSI
            }
        }
    }

    private fun handleAllowWidget() = withState { state ->
        viewModelScope.launch {
            if (state.permissionData()?.isWebviewWidget.orFalse()) {
                WidgetPermissionsHelper(integrationManagerService, widgetService).changePermission(state.roomId, state.widgetId, true)
            } else {
                //TODO JITSI
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
