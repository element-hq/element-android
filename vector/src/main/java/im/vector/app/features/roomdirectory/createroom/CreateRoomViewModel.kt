/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.createroom

import androidx.core.net.toFile
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.android.sdk.api.session.events.model.TchapEventType.STATE_ROOM_ACCESS_RULES
import fr.gouv.tchap.android.sdk.api.session.room.model.RoomAccessRules
import fr.gouv.tchap.android.sdk.api.session.room.model.RoomAccessRulesContent
import fr.gouv.tchap.core.utils.TchapRoomType
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns.getDomain
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomPreset
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomStateEvent
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber

class CreateRoomViewModel @AssistedInject constructor(@Assisted private val initialState: CreateRoomViewState,
                                                      private val session: Session,
                                                      private val rawService: RawService,
                                                      vectorPreferences: VectorPreferences
) : VectorViewModel<CreateRoomViewState, CreateRoomAction, CreateRoomViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CreateRoomViewModel, CreateRoomViewState> {
        override fun create(initialState: CreateRoomViewState): CreateRoomViewModel
    }

    companion object : MavericksViewModelFactory<CreateRoomViewModel, CreateRoomViewState> by hiltMavericksViewModelFactory() {
        private const val AGENT_SERVER_DOMAIN = "Agent"
    }

    init {
        initHomeServerName()
        initUserDomain()
        initAdminE2eByDefault()

        val restrictedSupport = session.getHomeServerCapabilities().isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED)
        val createRestricted = when (restrictedSupport) {
            HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED          -> true
            HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED_UNSTABLE -> vectorPreferences.labsUseExperimentalRestricted()
            else                                                            -> false
        }

        val defaultJoinRules = if (initialState.parentSpaceId != null && createRestricted) {
            RoomJoinRules.RESTRICTED
        } else {
            RoomJoinRules.INVITE
        }

        setState {
            copy(
                    supportsRestricted = createRestricted,
                    roomJoinRules = defaultJoinRules,
                    parentSpaceSummary = initialState.parentSpaceId?.let { session.getRoomSummary(it) }
            )
        }
    }

    private fun initHomeServerName() {
        setState {
            copy(
                    homeServerName = session.myUserId.getDomain()
            )
        }
    }

    private fun initUserDomain() {
        val displayName = session.run { getUser(myUserId) }?.toMatrixItem()?.getBestName().orEmpty()

        setState { copy(userDomain = TchapUtils.getDomainFromDisplayName(displayName)) }
    }

    private var adminE2EByDefault = true

    private fun initAdminE2eByDefault() {
        viewModelScope.launch(Dispatchers.IO) {
            adminE2EByDefault = tryOrNull {
                rawService.getElementWellknown(session.sessionParams)
                        ?.isE2EByDefault()
                        ?: true
            } ?: true

            setState {
                copy(
                        hsAdminHasDisabledE2E = !adminE2EByDefault,
                        defaultEncrypted = mapOf(
                                RoomJoinRules.INVITE to adminE2EByDefault,
                                RoomJoinRules.PUBLIC to false,
                                RoomJoinRules.RESTRICTED to adminE2EByDefault
                        )

                )
            }
        }
    }

    override fun handle(action: CreateRoomAction) {
        when (action) {
            is CreateRoomAction.SetAvatar             -> setAvatar(action)
            is CreateRoomAction.SetName               -> setName(action)
            is CreateRoomAction.SetTopic              -> setTopic(action)
            is CreateRoomAction.SetVisibility         -> setVisibility(action)
            is CreateRoomAction.SetTchapRoomType      -> setTchapRoomType(action)
            is CreateRoomAction.SetRoomAliasLocalPart -> setRoomAliasLocalPart(action)
            is CreateRoomAction.SetIsEncrypted        -> setIsEncrypted(action)
            is CreateRoomAction.Create                -> doCreateRoom()
            CreateRoomAction.Reset                    -> doReset()
            CreateRoomAction.ToggleShowAdvanced       -> toggleShowAdvanced()
            is CreateRoomAction.DisableFederation     -> disableFederation(action)
        }.exhaustive
    }

    private fun disableFederation(action: CreateRoomAction.DisableFederation) {
        setState {
            copy(disableFederation = action.disableFederation)
        }
    }

    private fun toggleShowAdvanced() {
        setState {
            copy(
                    showAdvanced = !showAdvanced,
                    // Reset to false if advanced is hidden
                    disableFederation = disableFederation && !showAdvanced
            )
        }
    }

    private fun doReset() {
        setState {
            // Delete temporary file with the avatar
            avatarUri?.let { tryOrNull { it.toFile().delete() } }

            CreateRoomViewState(
                    isEncrypted = adminE2EByDefault,
                    hsAdminHasDisabledE2E = !adminE2EByDefault,
                    parentSpaceId = initialState.parentSpaceId
            )
        }

        _viewEvents.post(CreateRoomViewEvents.Quit)
    }

    private fun setAvatar(action: CreateRoomAction.SetAvatar) = setState { copy(avatarUri = action.imageUri) }

    private fun setName(action: CreateRoomAction.SetName) = setState { copy(roomName = action.name) }

    private fun setTopic(action: CreateRoomAction.SetTopic) = setState { copy(roomTopic = action.topic) }

    private fun setVisibility(action: CreateRoomAction.SetVisibility) = setState {
        when (action.rule) {
            RoomJoinRules.PUBLIC     -> {
                copy(
                        roomJoinRules = RoomJoinRules.PUBLIC,
                        // Reset any error in the form about alias
                        asyncCreateRoomRequest = Uninitialized,
                        isEncrypted = false
                )
            }
            RoomJoinRules.RESTRICTED -> {
                copy(
                        roomJoinRules = RoomJoinRules.RESTRICTED,
                        // Reset any error in the form about alias
                        asyncCreateRoomRequest = Uninitialized,
                        isEncrypted = adminE2EByDefault
                )
            }
//            RoomJoinRules.INVITE,
//            RoomJoinRules.KNOCK,
//            RoomJoinRules.PRIVATE,
            else                     -> {
                // default to invite
                copy(
                        roomJoinRules = RoomJoinRules.INVITE,
                        isEncrypted = adminE2EByDefault
                )
            }
        }
    }

    private fun setTchapRoomType(action: CreateRoomAction.SetTchapRoomType) = setState {
        if (action.roomType == TchapRoomType.FORUM) {
            val userHSDomain = TchapUtils.getHomeServerDisplayNameFromMXIdentifier(session.myUserId)
            val isAgentServerDomain = userHSDomain.equals(AGENT_SERVER_DOMAIN, ignoreCase = true)
            copy(
                    roomType = action.roomType,
                    disableFederation = !isAgentServerDomain,
                    isFederationSettingAvailable = !isAgentServerDomain
            )
        } else {
            copy(
                    roomType = action.roomType,
                    disableFederation = false
            )
        }
    }

    private fun setRoomAliasLocalPart(action: CreateRoomAction.SetRoomAliasLocalPart) {
        setState {
            copy(
                    aliasLocalPart = action.aliasLocalPart,
                    // Reset any error in the form about alias
                    asyncCreateRoomRequest = Uninitialized
            )
        }
    }

    private fun setIsEncrypted(action: CreateRoomAction.SetIsEncrypted) = setState { copy(isEncrypted = action.isEncrypted) }

    private fun doCreateRoom() = withState { state ->
        if (state.asyncCreateRoomRequest is Loading || state.asyncCreateRoomRequest is Success) {
            return@withState
        }

        setState {
            copy(asyncCreateRoomRequest = Loading())
        }

        val createRoomParams = CreateRoomParams()
                .apply {
                    name = state.roomName.takeIf { it.isNotBlank() }
                    topic = state.roomTopic.takeIf { it.isNotBlank() }
                    avatarUri = state.avatarUri

                    if (state.isSubSpace) {
                        // Space-rooms are distinguished from regular messaging rooms by the m.room.type of m.space
                        roomType = RoomType.SPACE

                        // Space-rooms should be created with a power level for events_default of 100,
                        // to prevent the rooms accidentally/maliciously clogging up with messages from random members of the space.
                        powerLevelContentOverride = PowerLevelsContent(
                                eventsDefault = 100
                        )
                    }

                    if (state.roomType == TchapRoomType.FORUM) {
                        // Directory visibility
                        visibility = RoomDirectoryVisibility.PUBLIC
                        // Preset
                        preset = CreateRoomPreset.PRESET_PUBLIC_CHAT
                        // In case of a public room, the room alias is mandatory.
                        // That's why, we deduce the room alias from the room name.
                        roomAliasName = TchapUtils.createRoomAliasName(state.roomName)
                        historyVisibility = RoomHistoryVisibility.WORLD_READABLE
                    } else {
                        // Directory visibility
                        visibility = RoomDirectoryVisibility.PRIVATE
                        // Preset
                        preset = CreateRoomPreset.PRESET_PRIVATE_CHAT
                        // Hide the encrypted messages sent before the member is invited.
                        historyVisibility = RoomHistoryVisibility.INVITED
                        // Encryption
                        enableEncryption()
                    }

                    if (state.roomType == TchapRoomType.EXTERNAL) {
                        // Room access rule
                        setRoomAccessRulesInInitialStates(this, RoomAccessRules.UNRESTRICTED)
                    } else {
                        // Room access rule
                        setRoomAccessRulesInInitialStates(this, RoomAccessRules.RESTRICTED)
                    }

                    // Disabling federation
                    disableFederation = state.disableFederation
                }

        // TODO: Should this be non-cancellable?
        viewModelScope.launch {
            runCatching { session.createRoom(createRoomParams) }.fold(
                    { roomId ->

                        if (initialState.parentSpaceId != null) {
                            // add it as a child
                            try {
                                session.spaceService()
                                        .getSpace(initialState.parentSpaceId)
                                        ?.addChildren(roomId, viaServers = null, order = null)
                            } catch (failure: Throwable) {
                                Timber.w(failure, "Failed to add as a child")
                            }
                        }

                        setState {
                            copy(asyncCreateRoomRequest = Success(roomId))
                        }
                    },
                    { failure ->
                        setState {
                            copy(asyncCreateRoomRequest = Fail(failure))
                        }
                        _viewEvents.post(CreateRoomViewEvents.Failure(failure))
                    }
            )
        }
    }

    /**
     * Force the room access rule in the room creation parameters.
     *
     * @param roomParams the room creation parameters.
     * @param roomAccessRules the expected room access rules, set null to remove any existing value.
     */
    private fun setRoomAccessRulesInInitialStates(roomParams: CreateRoomParams, roomAccessRules: RoomAccessRules?) {
        // Remove the existing value if any.
        roomParams.initialStates.removeAll { it.type == STATE_ROOM_ACCESS_RULES }
        if (roomAccessRules != null) {
            val roomAccessRulesEvent = CreateRoomStateEvent(
                    STATE_ROOM_ACCESS_RULES,
                    RoomAccessRulesContent(
                            roomAccessRules.value
                    ).toContent()
            )
            roomParams.initialStates.add(roomAccessRulesEvent)
        }
    }
}
