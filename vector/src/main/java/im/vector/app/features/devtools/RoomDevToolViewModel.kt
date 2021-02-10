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

package im.vector.app.features.devtools

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.moshi.Types
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.rx.rx

class RoomDevToolViewModel @AssistedInject constructor(
        @Assisted val initialState: RoomDevToolViewState,
        private val errorFormatter: ErrorFormatter,
        private val session: Session
) : VectorViewModel<RoomDevToolViewState, RoomDevToolAction, DevToolsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomDevToolViewState): RoomDevToolViewModel
    }

    companion object : MvRxViewModelFactory<RoomDevToolViewModel, RoomDevToolViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomDevToolViewState): RoomDevToolViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {

        session.getRoom(initialState.roomId)?.rx()
                ?.liveStateEvents(emptySet())
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
                val jsonString = MoshiProvider.providesMoshi()
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
                            sendEventDraft = RoomDevToolViewState.SendEventDraft("m.room.message", null, "{\n}")
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

    private fun handleMenuItemSend() = withState {
        when (it.displayMode) {
            RoomDevToolViewState.Mode.EditEventContent -> {
                setState { copy(modalLoading = Loading()) }
                viewModelScope.launch {
                    try {
                        val room = session.getRoom(initialState.roomId)
                                ?: throw IllegalArgumentException("Room not found")

                        val adapter = MoshiProvider.providesMoshi()
                                .adapter<JsonDict>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
                        val json = adapter.fromJson(it.editedContent ?: "")
                                ?: throw IllegalArgumentException("No content")

                        room.sendStateEvent(
                                it.selectedEvent?.type ?: "",
                                it.selectedEvent?.stateKey,
                                json

                        )
                        _viewEvents.post(DevToolsViewEvents.showSnackMessage("State event sent!"))
                        setState {
                            copy(
                                    modalLoading = Success(Unit),
                                    selectedEventJson = null,
                                    editedContent = null,
                                    displayMode = RoomDevToolViewState.Mode.StateEventListByType
                            )
                        }
                    } catch (failure: Throwable) {
                        _viewEvents.post(DevToolsViewEvents.showAlertMessage(errorFormatter.toHumanReadable(failure)))
                        setState { copy(modalLoading = Fail(failure)) }
                    }
                }
            }
            is RoomDevToolViewState.Mode.SendEventForm -> {
                setState { copy(modalLoading = Loading()) }
                viewModelScope.launch {
                    try {
                        val room = session.getRoom(initialState.roomId)
                                ?: throw IllegalArgumentException("Room not found")

                        val adapter = MoshiProvider.providesMoshi()
                                .adapter<JsonDict>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
                        val json = adapter.fromJson(it.sendEventDraft?.content ?: "")
                                ?: throw IllegalArgumentException("No content")

                        val eventType = it.sendEventDraft?.type ?: throw IllegalArgumentException("Missing message type")
                        if (it.displayMode.isState) {
                            room.sendStateEvent(
                                    eventType,
                                    it.sendEventDraft.stateKey,
                                    json

                            )
                        } else {
                            // can we try to do some validation??
                            // val validParse = MoshiProvider.providesMoshi().adapter(MessageContent::class.java).fromJson(it.sendEventDraft.content ?: "")
                            json.toModel<MessageContent>(catchError = false)
                                    ?: throw IllegalArgumentException("Malformed event")
                            room.sendEvent(
                                    eventType,
                                    json
                            )
                        }

                        _viewEvents.post(DevToolsViewEvents.showSnackMessage("Event sent!"))
                        setState {
                            copy(
                                    modalLoading = Success(Unit),
                                    sendEventDraft = null,
                                    displayMode = RoomDevToolViewState.Mode.Root
                            )
                        }
                    } catch (failure: Throwable) {
                        _viewEvents.post(DevToolsViewEvents.showAlertMessage(errorFormatter.toHumanReadable(failure)))
                        setState { copy(modalLoading = Fail(failure)) }
                    }
                }
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
