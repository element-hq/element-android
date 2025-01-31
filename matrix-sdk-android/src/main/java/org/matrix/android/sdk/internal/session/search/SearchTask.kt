/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search

import org.matrix.android.sdk.api.session.search.EventAndSender
import org.matrix.android.sdk.api.session.search.SearchResult
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.search.request.SearchRequestBody
import org.matrix.android.sdk.internal.session.search.request.SearchRequestCategories
import org.matrix.android.sdk.internal.session.search.request.SearchRequestEventContext
import org.matrix.android.sdk.internal.session.search.request.SearchRequestFilter
import org.matrix.android.sdk.internal.session.search.request.SearchRequestOrder
import org.matrix.android.sdk.internal.session.search.request.SearchRequestRoomEvents
import org.matrix.android.sdk.internal.session.search.response.SearchResponse
import org.matrix.android.sdk.internal.session.search.response.SearchResponseItem
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface SearchTask : Task<SearchTask.Params, SearchResult> {

    data class Params(
            val searchTerm: String,
            val roomId: String,
            val nextBatch: String? = null,
            val orderByRecent: Boolean,
            val limit: Int,
            val beforeLimit: Int,
            val afterLimit: Int,
            val includeProfile: Boolean
    )
}

internal class DefaultSearchTask @Inject constructor(
        private val searchAPI: SearchAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val realmSessionProvider: RealmSessionProvider
) : SearchTask {

    override suspend fun execute(params: SearchTask.Params): SearchResult {
        val searchRequestBody = SearchRequestBody(
                searchCategories = SearchRequestCategories(
                        roomEvents = SearchRequestRoomEvents(
                                searchTerm = params.searchTerm,
                                orderBy = if (params.orderByRecent) SearchRequestOrder.RECENT else SearchRequestOrder.RANK,
                                filter = SearchRequestFilter(
                                        limit = params.limit,
                                        rooms = listOf(params.roomId)
                                ),
                                eventContext = SearchRequestEventContext(
                                        beforeLimit = params.beforeLimit,
                                        afterLimit = params.afterLimit,
                                        includeProfile = params.includeProfile
                                )
                        )
                )
        )
        return executeRequest(globalErrorReceiver) {
            searchAPI.search(params.nextBatch, searchRequestBody)
        }.toDomain()
    }

    private fun SearchResponse.toDomain(): SearchResult {
        val localTimelineEvents = findRootThreadEventsFromDB(searchCategories.roomEvents?.results)
        return SearchResult(
                nextBatch = searchCategories.roomEvents?.nextBatch,
                highlights = searchCategories.roomEvents?.highlights,
                results = searchCategories.roomEvents?.results?.map { searchResponseItem ->

                    val localThreadEventDetails = localTimelineEvents
                            ?.firstOrNull { it.eventId == searchResponseItem.event.eventId }
                            ?.root
                            ?.asDomain()
                            ?.threadDetails

                    EventAndSender(
                            searchResponseItem.event.apply {
                                threadDetails = localThreadEventDetails
                            },
                            searchResponseItem.event.senderId?.let { senderId ->
                                searchResponseItem.context?.profileInfo?.get(senderId)
                                        ?.let {
                                            MatrixItem.UserItem(
                                                    senderId,
                                                    it["displayname"] as? String,
                                                    it["avatar_url"] as? String
                                            )
                                        }
                            }
                    )
                }?.reversed()
        )
    }

    /**
     * Find local events if exists in order to enhance the result with thread summary.
     */
    private fun findRootThreadEventsFromDB(searchResponseItemList: List<SearchResponseItem>?): List<TimelineEventEntity>? {
        return realmSessionProvider.withRealm { realm ->
            searchResponseItemList?.mapNotNull {
                it.event.roomId ?: return@mapNotNull null
                it.event.eventId ?: return@mapNotNull null
                TimelineEventEntity.where(realm, it.event.roomId, it.event.eventId).findFirst()
            }?.filter {
                it.root?.isRootThread == true || it.root?.isThread() == true
            }
        }
    }
}
