/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import dagger.hilt.android.scopes.ActivityScoped
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.item.ReactionInfoData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryData
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

@ActivityScoped
class ReactionsSummaryFactory @Inject constructor() {

    var onRequestBuild: (() -> Unit)? = null
    private val showAllReactionsByEvent = HashSet<String>()
    private val eventsRequestingBuild = HashSet<String>()

    fun needsRebuild(event: TimelineEvent): Boolean {
        return eventsRequestingBuild.remove(event.eventId)
    }

    fun create(event: TimelineEvent): ReactionsSummaryData {
        val eventId = event.eventId
        val showAllStates = showAllReactionsByEvent.contains(eventId)
        val reactions = event.annotations?.reactionsSummary
                ?.map {
                    ReactionInfoData(it.key, it.count, it.addedByMe, it.localEchoEvents.isEmpty())
                }
        return ReactionsSummaryData(
                reactions = reactions,
                showAll = showAllStates
        )
    }

    fun onAddMoreClicked(callback: TimelineEventController.Callback?, event: TimelineEvent) {
        callback?.onAddMoreReaction(event)
    }

    fun onShowMoreClicked(eventId: String) {
        showAllReactionsByEvent.add(eventId)
        onRequestBuild(eventId)
    }

    fun onShowLessClicked(eventId: String) {
        showAllReactionsByEvent.remove(eventId)
        onRequestBuild(eventId)
    }

    private fun onRequestBuild(eventId: String) {
        eventsRequestingBuild.add(eventId)
        onRequestBuild?.invoke()
    }
}
