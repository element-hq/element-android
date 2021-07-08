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

package im.vector.app.features.roomprofile.settings.joinrule

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.rx.mapOptional
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap

data class RoomJoinRuleAdvancedState(
        val roomId: String,
        val summary: RoomSummary? = null,
        val currentRoomJoinRules: RoomJoinRules? = null,
        val initialAllowList: List<MatrixItem>? = null,
        val updatedAllowList: List<MatrixItem>? = null,
        val choices: Async<List<JoinRulesOptionSupport>> = Uninitialized
) : MvRxState {
    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
}

sealed class RoomJoinRuleAdvancedAction : VectorViewModelAction {
    data class SelectJoinRules(val rules: RoomJoinRules) : RoomJoinRuleAdvancedAction()
    data class UpdateAllowList(val roomIds: List<String>) : RoomJoinRuleAdvancedAction()
}

sealed class RoomJoinRuleAdvancedEvents : VectorViewEvents {
    object SelectAllowList : RoomJoinRuleAdvancedEvents()
}

class RoomJoinRuleAdvancedViewModel @AssistedInject constructor(
        @Assisted val initialState: RoomJoinRuleAdvancedState,
        private val session: Session,
        private val vectorPreferences: VectorPreferences
) : VectorViewModel<RoomJoinRuleAdvancedState, RoomJoinRuleAdvancedAction, RoomJoinRuleAdvancedEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)!!
    private val homeServerCapabilities = session.getHomeServerCapabilities()

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomJoinRuleAdvancedState): RoomJoinRuleAdvancedViewModel
    }

    companion object : MvRxViewModelFactory<RoomJoinRuleAdvancedViewModel, RoomJoinRuleAdvancedState> {

        override fun create(viewModelContext: ViewModelContext, state: RoomJoinRuleAdvancedState): RoomJoinRuleAdvancedViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    init {

        val initialAllowList = session.getRoom(initialState.roomId)?.getStateEvent(EventType.STATE_ROOM_JOIN_RULES, QueryStringValue.NoCondition)
                ?.content
                ?.toModel<RoomJoinRulesContent>()
                ?.allowList

        setState {
            val initialAllowItems = initialAllowList.orEmpty().map {
                session.getRoomSummary(it.spaceID)?.toMatrixItem()
                        ?: MatrixItem.RoomItem(it.spaceID, null, null)
            }
            copy(
                    summary = session.getRoomSummary(initialState.roomId),
                    initialAllowList = initialAllowItems,
                    updatedAllowList = initialAllowItems
            )
        }

        // TODO shouldn't be live
        room.rx()
                .liveStateEvent(EventType.STATE_ROOM_JOIN_RULES, QueryStringValue.NoCondition)
                .mapOptional { it.content.toModel<RoomJoinRulesContent>() }
                .unwrap()
                .subscribe { content ->

                    content.joinRules?.let {
                        var safeRule: RoomJoinRules = it
                        // server is not really checking that, just to be sure let's check
                        val restrictedSupportedByThisVersion = homeServerCapabilities
                                .isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED, room.getRoomVersion())
                        if (it == RoomJoinRules.RESTRICTED
                                && !restrictedSupportedByThisVersion) {
                            safeRule = RoomJoinRules.INVITE
                        }
                        val allowList = if (safeRule == RoomJoinRules.RESTRICTED) content.allowList else null

                        val restrictedSupport = homeServerCapabilities.isFeatureSupported(HomeServerCapabilities.ROOM_CAP_RESTRICTED)
                        val couldUpgradeToRestricted = when (restrictedSupport) {
                            HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED          -> true
                            HomeServerCapabilities.RoomCapabilitySupport.SUPPORTED_UNSTABLE -> vectorPreferences.labsUseExperimentalRestricted()
                            else                                                            -> false
                        }

                        val choices = if (restrictedSupportedByThisVersion || couldUpgradeToRestricted) {
                            listOf(
                                    RoomJoinRules.INVITE.toOption(false),
                                    RoomJoinRules.RESTRICTED.toOption(!restrictedSupportedByThisVersion),
                                    RoomJoinRules.PUBLIC.toOption(false)
                            )
                        } else {
                            listOf(
                                    RoomJoinRules.INVITE.toOption(false),
                                    RoomJoinRules.PUBLIC.toOption(false)
                            )
                        }

                        setState {
                            copy(
                                    currentRoomJoinRules = safeRule,
                                    choices = Success(choices)
                            )
                        }
                    }
                }
                .disposeOnClear()
    }

    override fun handle(action: RoomJoinRuleAdvancedAction) {
        when (action) {
            is RoomJoinRuleAdvancedAction.SelectJoinRules -> handleSelectRule(action)
            is RoomJoinRuleAdvancedAction.UpdateAllowList -> handleUpdateAllowList(action)
        }
    }

    fun handleUpdateAllowList(action: RoomJoinRuleAdvancedAction.UpdateAllowList) = withState { state ->
        setState {
            copy(
                    updatedAllowList = action.roomIds.map {
                        session.getRoomSummary(it)?.toMatrixItem() ?: MatrixItem.RoomItem(it, null, null)
                    }
            )
        }
    }

    fun handleSelectRule(action: RoomJoinRuleAdvancedAction.SelectJoinRules) = withState { state ->

        if (action.rules == RoomJoinRules.RESTRICTED) {
            // open space select?
            _viewEvents.post(RoomJoinRuleAdvancedEvents.SelectAllowList)
        }
        setState {
            copy(
                    currentRoomJoinRules = action.rules
            )
        }
    }
}
