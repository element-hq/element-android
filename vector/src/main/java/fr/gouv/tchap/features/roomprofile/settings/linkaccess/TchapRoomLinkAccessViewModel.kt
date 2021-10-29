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

package fr.gouv.tchap.features.roomprofile.settings.linkaccess

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.powerlevel.PowerLevelsObservableFactory
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.rx.mapOptional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap
import timber.log.Timber

class TchapRoomLinkAccessViewModel @AssistedInject constructor(
        @Assisted initialState: TchapRoomLinkAccessState,
        private val session: Session
) : VectorViewModel<TchapRoomLinkAccessState, TchapRoomLinkAccessAction, TchapRoomLinkAccessViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: TchapRoomLinkAccessState): TchapRoomLinkAccessViewModel
    }

    companion object : MvRxViewModelFactory<TchapRoomLinkAccessViewModel, TchapRoomLinkAccessState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: TchapRoomLinkAccessState): TchapRoomLinkAccessViewModel {
            val fragment: TchapRoomLinkAccessFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        observePowerLevel()
        observeJoinRule()
        observeRoomCanonicalAlias()
    }

    private fun observeRoomCanonicalAlias() {
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_CANONICAL_ALIAS, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomCanonicalAliasContent>() }
                .unwrap()
                .subscribe {
                    setState {
                        copy(
                                canonicalAlias = it.canonicalAlias,
                                alternativeAliases = it.alternativeAliases.orEmpty().sorted()
                        )
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: TchapRoomLinkAccessAction) {
        when (action) {
            is TchapRoomLinkAccessAction.SetIsEnabled -> handleSetIsEnabled(action)
        }.exhaustive
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun observePowerLevel() {
        PowerLevelsObservableFactory(room)
                .createObservable()
                .subscribe {
                    val powerLevelsHelper = PowerLevelsHelper(it)
                    setState {
                        copy(canChangeLinkAccess = powerLevelsHelper.isUserAllowedToSend(session.myUserId, true, EventType.STATE_ROOM_CANONICAL_ALIAS))
                    }
                }
                .disposeOnClear()
    }

    private fun observeJoinRule() {
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_JOIN_RULES, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomJoinRulesContent>() }
                .unwrap()
                .subscribe {
                    it.joinRules?.let {
                        setState { copy(currentRoomJoinRules = it) }
                    }
                }
                .disposeOnClear()
    }

    private fun handleSetIsEnabled(action: TchapRoomLinkAccessAction.SetIsEnabled) {
        if (action.isEnabled) {
            enableRoomAccessByLink()
        } else {
            disableRoomAccessByLink()
        }
    }

    private fun enableRoomAccessByLink() {
        postLoading(true)
        withState { state ->
            viewModelScope.launch {
                try {
                    // Generate room alias if it does not already exists
                    if (state.canonicalAlias.isNullOrEmpty()) {
                        Timber.d("## updateCanonicalAlias")
                        val newCanonicalAlias = TchapUtils.createRoomAlias(session, state.roomSummary()?.name.orEmpty())
                        room.addAlias(TchapUtils.extractRoomAliasName(newCanonicalAlias))
                        room.updateCanonicalAlias(
                                newCanonicalAlias,
                                state.alternativeAliases + newCanonicalAlias
                        )
                    }

                    // Update room join rules
                    Timber.d("## enableRoomAccessByLink")
                    room.rx()
                            .updateJoinRule(RoomJoinRules.PUBLIC, GuestAccess.Forbidden)
                            .subscribe(
                                    { postLoading(false) },
                                    {
                                        Timber.e("## enableRoomAccessByLink: $it")
                                        postLoading(false)
                                        _viewEvents.post(TchapRoomLinkAccessViewEvents.Failure(it))
                                    }
                            )
                            .disposeOnClear()
                } catch (failure: Throwable) {
                    postLoading(false)
                    _viewEvents.post(TchapRoomLinkAccessViewEvents.Failure(failure))
                }
            }
        }
    }

    private fun disableRoomAccessByLink() {
        postLoading(true)
        Timber.d("## disableRoomAccessByLink")
        room.rx()
                .updateJoinRule(RoomJoinRules.INVITE, null)
                .subscribe(
                        { postLoading(false) },
                        {
                            Timber.e("## disableRoomAccessByLink: $it")
                            postLoading(false)
                            _viewEvents.post(TchapRoomLinkAccessViewEvents.Failure(it))
                        }
                )
                .disposeOnClear()
    }

    private fun postLoading(isLoading: Boolean) {
        setState { copy(isLoading = isLoading) }
    }
}
