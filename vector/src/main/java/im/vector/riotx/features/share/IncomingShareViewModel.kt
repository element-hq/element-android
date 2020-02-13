/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.share

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.jakewharton.rxrelay2.BehaviorRelay
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.query.QueryStringValue
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.roomSummaryQueryParams
import im.vector.matrix.rx.rx
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.attachments.filterNonPreviewables
import im.vector.riotx.features.attachments.filterPreviewables
import im.vector.riotx.features.home.room.list.ChronologicalRoomComparator
import java.util.concurrent.TimeUnit

class IncomingShareViewModel @AssistedInject constructor(@Assisted initialState: IncomingShareViewState,
                                                         private val session: Session,
                                                         private val chronologicalRoomComparator: ChronologicalRoomComparator)
    : VectorViewModel<IncomingShareViewState, IncomingShareAction, IncomingShareViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: IncomingShareViewState): IncomingShareViewModel
    }

    companion object : MvRxViewModelFactory<IncomingShareViewModel, IncomingShareViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: IncomingShareViewState): IncomingShareViewModel? {
            val fragment: IncomingShareFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.incomingShareViewModelFactory.create(state)
        }
    }

    private val filterStream: BehaviorRelay<String> = BehaviorRelay.createDefault("")

    init {
        observeRoomSummaries()
    }

    private fun observeRoomSummaries() {
        val queryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
        }
        session
                .rx().liveRoomSummaries(queryParams)
                .execute {
                    copy(roomSummaries = it)
                }

        filterStream
                .switchMap { filter ->
                    val displayNameQuery = if (filter.isEmpty()) {
                        QueryStringValue.NoCondition
                    } else {
                        QueryStringValue.Contains(filter, QueryStringValue.Case.INSENSITIVE)
                    }
                    val filterQueryParams = roomSummaryQueryParams {
                        displayName = displayNameQuery
                        memberships = listOf(Membership.JOIN)
                    }
                    session.rx().liveRoomSummaries(filterQueryParams)
                }
                .throttleLast(300, TimeUnit.MILLISECONDS)
                .map { it.sortedWith(chronologicalRoomComparator) }
                .execute {
                    copy(filteredRoomSummaries = it)
                }
    }

    override fun handle(action: IncomingShareAction) {
        when (action) {
            is IncomingShareAction.SelectRoom           -> handleSelectRoom(action)
            is IncomingShareAction.ShareToSelectedRooms -> handleShareToSelectedRooms()
            is IncomingShareAction.ShareMedia           -> handleShareMediaToSelectedRooms(action)
            is IncomingShareAction.FilterWith           -> handleFilter(action)
            is IncomingShareAction.UpdateSharedData     -> handleUpdateSharedData(action)
        }.exhaustive
    }

    private fun handleUpdateSharedData(action: IncomingShareAction.UpdateSharedData) {
        setState { copy(sharedData = action.sharedData) }
    }

    private fun handleFilter(action: IncomingShareAction.FilterWith) {
        filterStream.accept(action.filter)
    }

    private fun handleShareToSelectedRooms() = withState { state ->
        val sharedData = state.sharedData ?: return@withState
        if (state.selectedRoomIds.size == 1) {
            // In this case the edition of the media will be handled by the RoomDetailFragment
            val selectedRoomId = state.selectedRoomIds.first()
            val selectedRoom = state.roomSummaries()?.find { it.roomId == selectedRoomId } ?: return@withState
            _viewEvents.post(IncomingShareViewEvents.ShareToRoom(selectedRoom, sharedData, showAlert = false))
        } else {
            when (sharedData) {
                is SharedData.Text        -> {
                    state.selectedRoomIds.forEach { roomId ->
                        val room = session.getRoom(roomId)
                        room?.sendTextMessage(sharedData.text)
                    }
                }
                is SharedData.Attachments -> {
                    shareAttachments(sharedData.attachmentData, state.selectedRoomIds, proposeMediaEdition = true, compressMediaBeforeSending = false)
                }
            }
        }
    }

    private fun shareAttachments(attachmentData: List<ContentAttachmentData>,
                                 selectedRoomIds: Set<String>,
                                 proposeMediaEdition: Boolean,
                                 compressMediaBeforeSending: Boolean) {
        if (!proposeMediaEdition) {
            // Pick the first room to send the media
            selectedRoomIds.firstOrNull()
                    ?.let { roomId -> session.getRoom(roomId) }
                    ?.sendMedias(attachmentData, compressMediaBeforeSending, selectedRoomIds)
        } else {
            val previewable = attachmentData.filterPreviewables()
            val nonPreviewable = attachmentData.filterNonPreviewables()
            if (nonPreviewable.isNotEmpty()) {
                // Send the non previewable attachment right now (?)
                // Pick the first room to send the media
                selectedRoomIds.firstOrNull()
                        ?.let { roomId -> session.getRoom(roomId) }
                        ?.sendMedias(nonPreviewable, compressMediaBeforeSending, selectedRoomIds)
            }
            if (previewable.isNotEmpty()) {
                // In case of multiple share of media, edit them first
                _viewEvents.post(IncomingShareViewEvents.EditMediaBeforeSending(previewable))
            }
        }
    }

    private fun handleShareMediaToSelectedRooms(action: IncomingShareAction.ShareMedia) = withState { state ->
        (state.sharedData as? SharedData.Attachments)?.let {
            shareAttachments(it.attachmentData, state.selectedRoomIds, proposeMediaEdition = false, compressMediaBeforeSending = !action.keepOriginalSize)
        }
    }

    private fun handleSelectRoom(action: IncomingShareAction.SelectRoom) = withState { state ->
        if (state.isInMultiSelectionMode) {
            val selectedRooms = state.selectedRoomIds
            val newSelectedRooms = if (selectedRooms.contains(action.roomSummary.roomId)) {
                selectedRooms.minus(action.roomSummary.roomId)
            } else {
                selectedRooms.plus(action.roomSummary.roomId)
            }
            setState { copy(isInMultiSelectionMode = newSelectedRooms.isNotEmpty(), selectedRoomIds = newSelectedRooms) }
        } else if (action.enableMultiSelect) {
            setState { copy(isInMultiSelectionMode = true, selectedRoomIds = setOf(action.roomSummary.roomId)) }
        } else {
            val sharedData = state.sharedData ?: return@withState
            _viewEvents.post(IncomingShareViewEvents.ShareToRoom(action.roomSummary, sharedData, showAlert = true))
        }
    }
}
