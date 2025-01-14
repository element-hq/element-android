/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.share

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.toggle
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.attachments.isPreviewable
import im.vector.app.features.attachments.toGroupedContentAttachmentData
import im.vector.app.features.home.room.list.BreadcrumbsRoomComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.flow.flow

class IncomingShareViewModel @AssistedInject constructor(
        @Assisted initialState: IncomingShareViewState,
        private val session: Session,
        private val breadcrumbsRoomComparator: BreadcrumbsRoomComparator
) :
        VectorViewModel<IncomingShareViewState, IncomingShareAction, IncomingShareViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<IncomingShareViewModel, IncomingShareViewState> {
        override fun create(initialState: IncomingShareViewState): IncomingShareViewModel
    }

    companion object : MavericksViewModelFactory<IncomingShareViewModel, IncomingShareViewState> by hiltMavericksViewModelFactory()

    private val filterStream = MutableStateFlow("")

    init {
        observeRoomSummaries()
    }

    private fun observeRoomSummaries() {
        val queryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.JOIN)
        }
        session
                .flow().liveRoomSummaries(queryParams)
                .execute {
                    copy(roomSummaries = it)
                }

        filterStream
                .flatMapLatest { filter ->
                    val displayNameQuery = if (filter.isEmpty()) {
                        QueryStringValue.NoCondition
                    } else {
                        QueryStringValue.Contains(filter, QueryStringValue.Case.INSENSITIVE)
                    }
                    val filterQueryParams = roomSummaryQueryParams {
                        displayName = displayNameQuery
                        memberships = listOf(Membership.JOIN)
                    }
                    session.flow().liveRoomSummaries(filterQueryParams)
                }
                .sample(300)
                .map { it.sortedWith(breadcrumbsRoomComparator) }
                .execute {
                    copy(filteredRoomSummaries = it)
                }
    }

    override fun handle(action: IncomingShareAction) {
        when (action) {
            is IncomingShareAction.SelectRoom -> handleSelectRoom(action)
            is IncomingShareAction.ShareToSelectedRooms -> handleShareToSelectedRooms()
            is IncomingShareAction.ShareToRoom -> handleShareToRoom(action)
            is IncomingShareAction.ShareMedia -> handleShareMediaToSelectedRooms(action)
            is IncomingShareAction.FilterWith -> handleFilter(action)
            is IncomingShareAction.UpdateSharedData -> handleUpdateSharedData(action)
        }
    }

    private fun handleUpdateSharedData(action: IncomingShareAction.UpdateSharedData) {
        setState { copy(sharedData = action.sharedData) }
    }

    private fun handleFilter(action: IncomingShareAction.FilterWith) {
        filterStream.tryEmit(action.filter)
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
                is SharedData.Text -> {
                    state.selectedRoomIds.forEach { roomId ->
                        val room = session.getRoom(roomId)
                        room?.sendService()?.sendTextMessage(sharedData.text)
                    }
                    // This is it, pass the first roomId to let the screen open it
                    _viewEvents.post(IncomingShareViewEvents.MultipleRoomsShareDone(state.selectedRoomIds.first()))
                }
                is SharedData.Attachments -> {
                    shareAttachments(sharedData.attachmentData, state.selectedRoomIds, proposeMediaEdition = true, compressMediaBeforeSending = false)
                }
            }
        }
    }

    private fun handleShareToRoom(action: IncomingShareAction.ShareToRoom) = withState { state ->
        val sharedData = state.sharedData ?: return@withState
        val roomSummary = session.getRoomSummary(action.roomId) ?: return@withState
        _viewEvents.post(IncomingShareViewEvents.ShareToRoom(roomSummary, sharedData, showAlert = false))
    }

    private fun handleShareMediaToSelectedRooms(action: IncomingShareAction.ShareMedia) = withState { state ->
        (state.sharedData as? SharedData.Attachments)?.let {
            shareAttachments(it.attachmentData, state.selectedRoomIds, proposeMediaEdition = false, compressMediaBeforeSending = !action.keepOriginalSize)
        }
    }

    private fun shareAttachments(
            attachmentData: List<ContentAttachmentData>,
            selectedRoomIds: Set<String>,
            proposeMediaEdition: Boolean,
            compressMediaBeforeSending: Boolean
    ) {
        if (proposeMediaEdition) {
            val grouped = attachmentData.toGroupedContentAttachmentData()
            if (grouped.notPreviewables.isNotEmpty()) {
                // Send the not previewable attachments right now (?)
                // Pick the first room to send the media
                selectedRoomIds.firstOrNull()
                        ?.let { roomId -> session.getRoom(roomId) }
                        ?.sendService()
                        ?.sendMedias(grouped.notPreviewables, compressMediaBeforeSending, selectedRoomIds)

                // Ensure they will not be sent twice
                setState {
                    copy(
                            sharedData = SharedData.Attachments(grouped.previewables)
                    )
                }
            }
            if (grouped.previewables.isNotEmpty()) {
                // In case of multiple share of media, edit them first
                _viewEvents.post(IncomingShareViewEvents.EditMediaBeforeSending(grouped.previewables))
            } else {
                // This is it, pass the first roomId to let the screen open it
                _viewEvents.post(IncomingShareViewEvents.MultipleRoomsShareDone(selectedRoomIds.first()))
            }
        } else {
            // Pick the first room to send the media
            selectedRoomIds.firstOrNull()
                    ?.let { roomId -> session.getRoom(roomId) }
                    ?.sendService()
                    ?.sendMedias(attachmentData, compressMediaBeforeSending, selectedRoomIds)
            // This is it, pass the first roomId to let the screen open it
            _viewEvents.post(IncomingShareViewEvents.MultipleRoomsShareDone(selectedRoomIds.first()))
        }
    }

    private fun handleSelectRoom(action: IncomingShareAction.SelectRoom) = withState { state ->
        if (state.isInMultiSelectionMode) {
            // One room is clicked (or long clicked) while in multi selection mode -> toggle this room
            val selectedRooms = state.selectedRoomIds
            val newSelectedRooms = selectedRooms.toggle(action.roomSummary.roomId)
            setState { copy(isInMultiSelectionMode = newSelectedRooms.isNotEmpty(), selectedRoomIds = newSelectedRooms) }
        } else if (action.enableMultiSelect) {
            // One room is long clicked, not in multi selection mode -> enable multi selection mode
            setState { copy(isInMultiSelectionMode = true, selectedRoomIds = setOf(action.roomSummary.roomId)) }
        } else {
            // One room is clicked, not in multi selection mode -> direct share
            val sharedData = state.sharedData ?: return@withState
            val doNotShowAlert = when (sharedData) {
                is SharedData.Attachments -> {
                    // Do not show alert if the shared data contains only previewable attachments, because the user will get another chance to cancel the share
                    sharedData.attachmentData.all { it.isPreviewable() }
                }
                is SharedData.Text -> {
                    // Do not show alert when sharing text to one room, because it will just fill the composer
                    true
                }
            }
            _viewEvents.post(IncomingShareViewEvents.ShareToRoom(action.roomSummary, sharedData, !doNotShowAlert))
        }
    }
}
