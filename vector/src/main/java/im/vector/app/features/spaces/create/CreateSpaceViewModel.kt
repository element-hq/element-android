/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.create

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.Interaction
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.extensions.isEmail
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceListener
import org.matrix.android.sdk.api.session.room.AliasAvailabilityResult
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure

class CreateSpaceViewModel @AssistedInject constructor(
        @Assisted initialState: CreateSpaceState,
        private val session: Session,
        private val stringProvider: StringProvider,
        private val createSpaceViewModelTask: CreateSpaceViewModelTask,
        private val errorFormatter: ErrorFormatter,
        private val analyticsTracker: AnalyticsTracker,
) : VectorViewModel<CreateSpaceState, CreateSpaceAction, CreateSpaceEvents>(initialState) {

    private val identityService = session.identityService()

    private val identityServerManagerListener = object : IdentityServiceListener {
        override fun onIdentityServerChange() {
            val identityServerUrl = identityService.getCurrentIdentityServerUrl()
            setState {
                copy(
                        canInviteByMail = identityServerUrl != null
                )
            }
        }
    }

    init {
        val identityServerUrl = identityService.getCurrentIdentityServerUrl()
        setState {
            copy(
                    homeServerName = session.myUserId.getServerName(),
                    canInviteByMail = identityServerUrl != null
            )
        }
        startListenToIdentityManager()
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CreateSpaceViewModel, CreateSpaceState> {
        override fun create(initialState: CreateSpaceState): CreateSpaceViewModel
    }

    private fun startListenToIdentityManager() {
        identityService.addListener(identityServerManagerListener)
    }

    private fun stopListenToIdentityManager() {
        identityService.removeListener(identityServerManagerListener)
    }

    override fun onCleared() {
        stopListenToIdentityManager()
        super.onCleared()
    }

    companion object : MavericksViewModelFactory<CreateSpaceViewModel, CreateSpaceState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): CreateSpaceState {
            return CreateSpaceState(
                    defaultRooms = mapOf(
                            0 to viewModelContext.activity.getString(CommonStrings.create_spaces_default_public_room_name),
                            1 to viewModelContext.activity.getString(CommonStrings.create_spaces_default_public_random_room_name)
                    )
            )
        }
    }

    override fun handle(action: CreateSpaceAction) {
        when (action) {
            is CreateSpaceAction.SetRoomType -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.SetDetails,
                            spaceType = action.type
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
            }
            is CreateSpaceAction.NameChanged -> {
                setState {
                    if (aliasManuallyModified) {
                        copy(
                                nameInlineError = null,
                                name = action.name,
                                aliasVerificationTask = Uninitialized
                        )
                    } else {
                        val tentativeAlias =
                                MatrixPatterns.candidateAliasFromRoomName(action.name, homeServerName)
                        copy(
                                nameInlineError = null,
                                name = action.name,
                                aliasLocalPart = tentativeAlias,
                                aliasVerificationTask = Uninitialized
                        )
                    }
                }
            }
            is CreateSpaceAction.TopicChanged -> {
                setState {
                    copy(
                            topic = action.topic
                    )
                }
            }
            is CreateSpaceAction.SpaceAliasChanged -> {
                // This called only when the alias is change manually
                // not when programmatically changed via a change on name
                setState {
                    copy(
                            aliasManuallyModified = true,
                            aliasLocalPart = action.aliasLocalPart,
                            aliasVerificationTask = Uninitialized
                    )
                }
            }
            CreateSpaceAction.OnBackPressed -> {
                handleBackNavigation()
            }
            CreateSpaceAction.NextFromDetails -> {
                handleNextFromDetails()
            }
            CreateSpaceAction.NextFromDefaultRooms -> {
                handleNextFromDefaultRooms()
            }
            CreateSpaceAction.NextFromAdd3pid -> {
                handleNextFrom3pid()
            }
            is CreateSpaceAction.DefaultRoomNameChanged -> {
                setState {
                    copy(
                            defaultRooms = defaultRooms.orEmpty().toMutableMap().apply {
                                this[action.index] = action.name
                            }
                    )
                }
            }
            is CreateSpaceAction.DefaultInvite3pidChanged -> {
                setState {
                    copy(
                            default3pidInvite = default3pidInvite.orEmpty().toMutableMap().apply {
                                this[action.index] = action.email
                            },
                            emailValidationResult = emailValidationResult.orEmpty().toMutableMap().apply {
                                this.remove(action.index)
                            }
                    )
                }
            }
            is CreateSpaceAction.SetAvatar -> {
                setState { copy(avatarUri = action.uri) }
            }
            is CreateSpaceAction.SetSpaceTopology -> {
                handleSetTopology(action)
            }
        }
    }

    private fun handleSetTopology(action: CreateSpaceAction.SetSpaceTopology) {
        when (action.topology) {
            SpaceTopology.JustMe -> {
                setState {
                    copy(
                            spaceTopology = SpaceTopology.JustMe,
                            defaultRooms = emptyMap()
                    )
                }
                handleNextFromDefaultRooms()
            }
            SpaceTopology.MeAndTeammates -> {
                setState {
                    copy(
                            spaceTopology = SpaceTopology.MeAndTeammates,
                            step = CreateSpaceState.Step.AddEmailsOrInvites
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToAdd3Pid)
            }
        }
    }

    private fun handleBackNavigation() = withState { state ->
        when (state.step) {
            CreateSpaceState.Step.ChooseType -> {
                _viewEvents.post(CreateSpaceEvents.Dismiss)
            }
            CreateSpaceState.Step.SetDetails -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChooseType,
                            nameInlineError = null,
                            creationResult = Uninitialized
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChooseType)
            }
            CreateSpaceState.Step.AddRooms -> {
                if (state.spaceType == SpaceType.Private && state.spaceTopology == SpaceTopology.MeAndTeammates) {
                    setState {
                        copy(
                                spaceTopology = null,
                                step = CreateSpaceState.Step.AddEmailsOrInvites
                        )
                    }
                    _viewEvents.post(CreateSpaceEvents.NavigateToAdd3Pid)
                } else {
                    setState {
                        copy(
                                step = CreateSpaceState.Step.SetDetails
                        )
                    }
                    _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
                }
            }
            CreateSpaceState.Step.ChoosePrivateType -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.SetDetails
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
            }
            CreateSpaceState.Step.AddEmailsOrInvites -> {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChoosePrivateType
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChoosePrivateType)
            }
        }
    }

    private fun handleNextFrom3pid() = withState { state ->
        // check if emails are valid
        val emailValidation = state.default3pidInvite?.mapValues {
            val email = it.value
            email.isNullOrEmpty() || email.isEmail()
        }
        if (emailValidation?.all { it.value } != false) {
            setState {
                copy(
                        step = CreateSpaceState.Step.AddRooms
                )
            }
            _viewEvents.post(CreateSpaceEvents.NavigateToAddRooms)
        } else {
            setState {
                copy(
                        emailValidationResult = emailValidation
                )
            }
        }
    }

    private fun handleNextFromDetails() = withState { state ->
        if (state.name.isNullOrBlank()) {
            setState {
                copy(
                        nameInlineError = stringProvider.getString(CommonStrings.create_space_error_empty_field_space_name)
                )
            }
        } else {
            if (state.spaceType == SpaceType.Private) {
                setState {
                    copy(
                            step = CreateSpaceState.Step.ChoosePrivateType
                    )
                }
                _viewEvents.post(CreateSpaceEvents.NavigateToChoosePrivateType)
            } else {
                // it'a public space, let's check alias
                val aliasLocalPart = state.aliasLocalPart
                _viewEvents.post(CreateSpaceEvents.ShowModalLoading(null))
                setState {
                    copy(aliasVerificationTask = Loading())
                }
                viewModelScope.launch {
                    try {
                        when (val result = session.roomDirectoryService().checkAliasAvailability(aliasLocalPart)) {
                            AliasAvailabilityResult.Available -> {
                                setState {
                                    copy(
                                            step = CreateSpaceState.Step.AddRooms
                                    )
                                }
                                _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                                _viewEvents.post(CreateSpaceEvents.NavigateToAddRooms)
                            }
                            is AliasAvailabilityResult.NotAvailable -> {
                                setState {
                                    copy(aliasVerificationTask = Fail(result.roomAliasError))
                                }
                                _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                            }
                        }
                    } catch (failure: Throwable) {
                        setState {
                            copy(aliasVerificationTask = Fail(failure))
                        }
                        _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                    }
                }
            }
        }
    }

    private fun handleNextFromDefaultRooms() = withState { state ->
        val spaceName = state.name ?: return@withState
        setState {
            copy(creationResult = Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                analyticsTracker.capture(
                        Interaction(
                                index = null,
                                interactionType = null,
                                name = Interaction.Name.MobileSpaceCreationValidated
                        )
                )
                val alias = if (state.spaceType == SpaceType.Public) {
                    state.aliasLocalPart
                } else null
                val result = createSpaceViewModelTask.execute(
                        CreateSpaceTaskParams(
                                spaceName = spaceName,
                                spaceTopic = state.topic,
                                spaceAvatar = state.avatarUri,
                                isPublic = state.spaceType == SpaceType.Public,
                                defaultRooms = state.defaultRooms
                                        ?.entries
                                        ?.sortedBy { it.key }
                                        ?.mapNotNull { it.value }
                                        .orEmpty(),
                                spaceAlias = alias,
                                defaultEmailToInvite = state.default3pidInvite
                                        ?.values
                                        ?.mapNotNull { it.takeIf { it?.isEmail() == true } }
                                        ?.takeIf { state.spaceTopology == SpaceTopology.MeAndTeammates }
                                        .orEmpty()
                        )
                )
                when (result) {
                    is CreateSpaceTaskResult.Success -> {
                        setState {
                            copy(creationResult = Success(result.spaceId))
                        }
                        _viewEvents.post(
                                CreateSpaceEvents.FinishSuccess(
                                        result.spaceId,
                                        result.childIds.firstOrNull(),
                                        state.spaceTopology
                                )
                        )
                    }
                    is CreateSpaceTaskResult.PartialSuccess -> {
                        // XXX what can we do here?
                        setState {
                            copy(creationResult = Success(result.spaceId))
                        }
                        _viewEvents.post(
                                CreateSpaceEvents.FinishSuccess(
                                        result.spaceId,
                                        result.childIds.firstOrNull(),
                                        state.spaceTopology
                                )
                        )
                    }
                    is CreateSpaceTaskResult.FailedToCreateSpace -> {
                        if (result.failure is CreateRoomFailure.AliasError) {
                            setState {
                                copy(
                                        step = CreateSpaceState.Step.SetDetails,
                                        aliasVerificationTask = Fail(result.failure.aliasError),
                                        creationResult = Uninitialized
                                )
                            }
                            _viewEvents.post(CreateSpaceEvents.HideModalLoading)
                            _viewEvents.post(CreateSpaceEvents.NavigateToDetails)
                        } else {
                            setState {
                                copy(creationResult = Fail(result.failure))
                            }
                            _viewEvents.post(CreateSpaceEvents.ShowModalError(errorFormatter.toHumanReadable(result.failure)))
                        }
                    }
                }
            } catch (failure: Throwable) {
                setState {
                    copy(creationResult = Fail(failure))
                }
                _viewEvents.post(CreateSpaceEvents.ShowModalError(errorFormatter.toHumanReadable(failure)))
            }
        }
    }
}
