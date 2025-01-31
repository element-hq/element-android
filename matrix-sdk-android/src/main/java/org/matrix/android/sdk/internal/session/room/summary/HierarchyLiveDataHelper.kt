/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.room.summary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.Optional

internal class HierarchyLiveDataHelper(
        val spaceId: String,
        val memberships: List<Membership>,
        val roomSummaryDataSource: RoomSummaryDataSource
) {

    private val sources = HashMap<String, LiveData<Optional<RoomSummary>>>()
    private val mediatorLiveData = MediatorLiveData<List<String>>()

    fun liveData(): LiveData<List<String>> = mediatorLiveData

    init {
        onChange()
    }

    private fun parentsToCheck(): List<RoomSummary> {
        val spaces = ArrayList<RoomSummary>()
        roomSummaryDataSource.getSpaceSummary(spaceId)?.let {
            roomSummaryDataSource.flattenSubSpace(it, emptyList(), spaces, memberships)
        }
        return spaces
    }

    private fun onChange() {
        val existingSources = sources.keys.toList()
        val newSources = parentsToCheck().map { it.roomId }
        val addedSources = newSources.filter { !existingSources.contains(it) }
        val removedSource = existingSources.filter { !newSources.contains(it) }
        addedSources.forEach {
            val liveData = roomSummaryDataSource.getSpaceSummaryLive(it)
            mediatorLiveData.addSource(liveData) { onChange() }
            sources[it] = liveData
        }

        removedSource.forEach {
            sources[it]?.let { mediatorLiveData.removeSource(it) }
        }

        sources[spaceId]?.value?.getOrNull()?.let { spaceSummary ->
            val results = ArrayList<RoomSummary>()
            roomSummaryDataSource.flattenChild(spaceSummary, emptyList(), results, memberships)
            mediatorLiveData.postValue(results.map { it.roomId })
        }
    }
}
