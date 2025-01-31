/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.pushrules

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isInvitation
import org.matrix.android.sdk.api.session.pushrules.PushEvents
import org.matrix.android.sdk.api.session.pushrules.rest.PushRule
import org.matrix.android.sdk.api.session.sync.model.RoomsSyncResponse
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.task.Task
import timber.log.Timber
import javax.inject.Inject

internal interface ProcessEventForPushTask : Task<ProcessEventForPushTask.Params, Unit> {
    data class Params(
            val syncResponse: RoomsSyncResponse,
            val rules: List<PushRule>
    )
}

internal class DefaultProcessEventForPushTask @Inject constructor(
        private val defaultPushRuleService: DefaultPushRuleService,
        private val pushRuleFinder: PushRuleFinder,
        @UserId private val userId: String
) : ProcessEventForPushTask {

    override suspend fun execute(params: ProcessEventForPushTask.Params) {
        val newJoinEvents = params.syncResponse.join
                .mapNotNull { (key, value) ->
                    value.timeline?.events?.mapNotNull {
                        it.takeIf { !it.isInvitation() }?.copy(roomId = key)
                    }
                }
                .flatten()

        val inviteEvents = params.syncResponse.invite
                .mapNotNull { (key, value) ->
                    value.inviteState?.events?.map { it.copy(roomId = key) }
                }
                .flatten()

        val allEvents = (newJoinEvents + inviteEvents).filter { event ->
            when (event.type) {
                in EventType.POLL_START,
                in EventType.STATE_ROOM_BEACON_INFO,
                EventType.MESSAGE,
                EventType.REDACTION,
                EventType.ENCRYPTED,
                EventType.STATE_ROOM_MEMBER -> true
                else -> false
            }
        }.filter {
            it.senderId != userId
        }
        Timber.v(
                "[PushRules] Found ${allEvents.size} out of ${(newJoinEvents + inviteEvents).size}" +
                        " to check for push rules with ${params.rules.size} rules"
        )
        val matchedEvents = allEvents.mapNotNull { event ->
            pushRuleFinder.fulfilledBingRule(event, params.rules)?.let {
                Timber.v("[PushRules] Rule $it match for event ${event.eventId}")
                event to it
            }
        }
        Timber.d("[PushRules] matched ${matchedEvents.size} out of ${allEvents.size}")

        val allRedactedEvents = params.syncResponse.join
                .asSequence()
                .mapNotNull { it.value.timeline?.events }
                .flatten()
                .filter { it.type == EventType.REDACTION }
                .mapNotNull { it.redacts }
                .toList()

        Timber.v("[PushRules] Found ${allRedactedEvents.size} redacted events")

        defaultPushRuleService.dispatchEvents(
                PushEvents(
                        matchedEvents = matchedEvents,
                        roomsJoined = params.syncResponse.join.keys,
                        roomsLeft = params.syncResponse.leave.keys,
                        redactedEventIds = allRedactedEvents
                )
        )
    }
}
