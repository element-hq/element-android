/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.devtools

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.squareup.moshi.Types
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.matrix.android.sdk.flow.flow

class RoomDevToolViewModel @AssistedInject constructor(
        @Assisted val initialState: RoomDevToolViewState,
        private val errorFormatter: ErrorFormatter,
        private val stringProvider: StringProvider,
        private val session: Session
) : VectorViewModel<RoomDevToolViewState, RoomDevToolAction, DevToolsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomDevToolViewModel, RoomDevToolViewState> {
        override fun create(initialState: RoomDevToolViewState): RoomDevToolViewModel
    }

    companion object : MavericksViewModelFactory<RoomDevToolViewModel, RoomDevToolViewState> by hiltMavericksViewModelFactory()

    init {
        session.getRoom(initialState.roomId)
                ?.flow()
                ?.liveStateEvents(emptySet(), QueryStringValue.IsNotNull)
                ?.execute { async ->
                    copy(stateEvents = async)
                }
    }

    override fun handle(action: RoomDevToolAction) {
        when (action) {
            RoomDevToolAction.ExploreRoomState -> {
                setState {
                    copy(
                            displayMode = RoomDevToolViewState.Mode.StateEventList,
                            selectedEvent = null
                    )
                }
            }
            is RoomDevToolAction.ShowStateEvent -> {
                val jsonString = MatrixJsonParser.getMoshi()
                        .adapter(Event::class.java)
                        .toJson(action.event)

                setState {
                    copy(
                            displayMode = RoomDevToolViewState.Mode.StateEventDetail,
                            selectedEvent = action.event,
                            selectedEventJson = jsonString
                    )
                }
            }
            RoomDevToolAction.OnBackPressed -> {
                handleBack()
            }
            RoomDevToolAction.MenuEdit -> {
                withState {
                    if (it.displayMode == RoomDevToolViewState.Mode.StateEventDetail) {
                        // we want to edit it
                        val content = it.selectedEvent?.content?.let { JSONObject(it).toString(4) } ?: "{\n\t\n}"
                        setState {
                            copy(
                                    editedContent = content,
                                    displayMode = RoomDevToolViewState.Mode.EditEventContent
                            )
                        }
                    }
                }
            }
            is RoomDevToolAction.ShowStateEventType -> {
                setState {
                    copy(
                            displayMode = RoomDevToolViewState.Mode.StateEventListByType,
                            currentStateType = action.stateEventType
                    )
                }
            }
            RoomDevToolAction.MenuItemSend -> {
                handleMenuItemSend()
            }
            is RoomDevToolAction.UpdateContentText -> {
                setState {
                    copy(editedContent = action.contentJson)
                }
            }
            is RoomDevToolAction.SendCustomEvent -> {
                setState {
                    copy(
                            displayMode = RoomDevToolViewState.Mode.SendEventForm(action.isStateEvent),
                            sendEventDraft = RoomDevToolViewState.SendEventDraft(EventType.MESSAGE, null, "{\n}")
                    )
                }
            }
            is RoomDevToolAction.CustomEventTypeChange -> {
                setState {
                    copy(
                            sendEventDraft = sendEventDraft?.copy(type = action.type)
                    )
                }
            }
            is RoomDevToolAction.CustomEventStateKeyChange -> {
                setState {
                    copy(
                            sendEventDraft = sendEventDraft?.copy(stateKey = action.stateKey)
                    )
                }
            }
            is RoomDevToolAction.CustomEventContentChange -> {
                setState {
                    copy(
                            sendEventDraft = sendEventDraft?.copy(content = action.content)
                    )
                }
            }
        }
    }

    private fun handleMenuItemSend() = withState { state ->
        when (state.displayMode) {
            RoomDevToolViewState.Mode.EditEventContent -> editEventContent(state)
            is RoomDevToolViewState.Mode.SendEventForm -> sendEventContent(state, state.displayMode.isState)
            else -> Unit
        }
    }

    private fun editEventContent(state: RoomDevToolViewState) {
        setState { copy(modalLoading = Loading()) }

        viewModelScope.launch {
            try {
                val room = session.getRoom(initialState.roomId)
                        ?: throw IllegalArgumentException(stringProvider.getString(CommonStrings.room_error_not_found))

                val adapter = MatrixJsonParser.getMoshi()
                        .adapter<JsonDict>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
                val json = adapter.fromJson(state.editedContent ?: "")
                        ?: throw IllegalArgumentException(stringProvider.getString(CommonStrings.dev_tools_error_no_content))

                room.stateService().sendStateEvent(
                        state.selectedEvent?.type.orEmpty(),
                        state.selectedEvent?.stateKey.orEmpty(),
                        json
                )
                _viewEvents.post(DevToolsViewEvents.ShowSnackMessage(stringProvider.getString(CommonStrings.dev_tools_success_state_event)))
                setState {
                    copy(
                            modalLoading = Success(Unit),
                            selectedEventJson = null,
                            editedContent = null,
                            displayMode = RoomDevToolViewState.Mode.StateEventListByType
                    )
                }
            } catch (failure: Throwable) {
                _viewEvents.post(DevToolsViewEvents.ShowAlertMessage(errorFormatter.toHumanReadable(failure)))
                setState { copy(modalLoading = Fail(failure)) }
            }
        }
    }

    private fun sendEventContent(state: RoomDevToolViewState, isState: Boolean) {
        setState { copy(modalLoading = Loading()) }
        viewModelScope.launch {
            try {
                val room = session.getRoom(initialState.roomId)
                        ?: throw IllegalArgumentException(stringProvider.getString(CommonStrings.room_error_not_found))

                val adapter = MatrixJsonParser.getMoshi()
                        .adapter<JsonDict>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
                val json = adapter.fromJson(state.sendEventDraft?.content ?: "")
                        ?: throw IllegalArgumentException(stringProvider.getString(CommonStrings.dev_tools_error_no_content))

                val eventType = state.sendEventDraft?.type
                        ?: throw IllegalArgumentException(stringProvider.getString(CommonStrings.dev_tools_error_no_message_type))

                if (isState) {
                    room.stateService().sendStateEvent(
                            eventType,
                            state.sendEventDraft.stateKey.orEmpty(),
                            json
                    )
                } else {
                    // can we try to do some validation??
                    // val validParse = MoshiProvider.providesMoshi().adapter(MessageContent::class.java).fromJson(it.sendEventDraft.content ?: "")
                    json.toModel<MessageContent>(catchError = false)
                            ?: throw IllegalArgumentException(stringProvider.getString(CommonStrings.dev_tools_error_malformed_event))
                    room.sendService().sendEvent(
                            eventType,
                            json
                    )
                }

                _viewEvents.post(DevToolsViewEvents.ShowSnackMessage(stringProvider.getString(CommonStrings.dev_tools_success_event)))
                setState {
                    copy(
                            modalLoading = Success(Unit),
                            sendEventDraft = null,
                            displayMode = RoomDevToolViewState.Mode.Root
                    )
                }
            } catch (failure: Throwable) {
                _viewEvents.post(DevToolsViewEvents.ShowAlertMessage(errorFormatter.toHumanReadable(failure)))
                setState { copy(modalLoading = Fail(failure)) }
            }
        }
    }

    private fun handleBack() = withState {
        when (it.displayMode) {
            RoomDevToolViewState.Mode.Root -> {
                _viewEvents.post(DevToolsViewEvents.Dismiss)
            }
            RoomDevToolViewState.Mode.StateEventList -> {
                setState {
                    copy(
                            selectedEvent = null,
                            selectedEventJson = null,
                            displayMode = RoomDevToolViewState.Mode.Root
                    )
                }
            }
            RoomDevToolViewState.Mode.StateEventDetail -> {
                setState {
                    copy(
                            selectedEvent = null,
                            selectedEventJson = null,
                            displayMode = RoomDevToolViewState.Mode.StateEventListByType
                    )
                }
            }
            RoomDevToolViewState.Mode.EditEventContent -> {
                setState {
                    copy(
                            displayMode = RoomDevToolViewState.Mode.StateEventDetail
                    )
                }
            }
            RoomDevToolViewState.Mode.StateEventListByType -> {
                setState {
                    copy(
                            currentStateType = null,
                            displayMode = RoomDevToolViewState.Mode.StateEventList
                    )
                }
            }
            is RoomDevToolViewState.Mode.SendEventForm -> {
                setState {
                    copy(
                            displayMode = RoomDevToolViewState.Mode.Root
                    )
                }
            }
        }
    }
}
