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
