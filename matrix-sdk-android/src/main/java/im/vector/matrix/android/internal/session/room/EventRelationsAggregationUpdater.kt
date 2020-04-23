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
package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.SqlLiveEntityObserver
import im.vector.matrix.sqldelight.session.EventInsertNotification
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

/**
 * Acts as a listener of incoming messages in order to incrementally computes a summary of annotations.
 * For reactions will build a EventAnnotationsSummaryEntity, ans for edits a EditAggregatedSummaryEntity.
 * The summaries can then be extracted and added (as a decoration) to a TimelineEvent for final display.
 */
internal class EventRelationsAggregationUpdater @Inject constructor(
        sessionDatabase: SessionDatabase,
        private val task: EventRelationsAggregationTask) :
        SqlLiveEntityObserver<EventInsertNotification>(sessionDatabase) {

    override val query = sessionDatabase.observerTriggerQueries.getAllEventInsertNotifications(
            types = listOf(
                    EventType.MESSAGE,
                    EventType.REDACTION,
                    EventType.REACTION,
                    EventType.KEY_VERIFICATION_DONE,
                    EventType.KEY_VERIFICATION_CANCEL,
                    EventType.KEY_VERIFICATION_ACCEPT,
                    EventType.KEY_VERIFICATION_START,
                    EventType.KEY_VERIFICATION_MAC,
                    // TODO Add ?
                    // EventType.KEY_VERIFICATION_READY,
                    EventType.KEY_VERIFICATION_KEY,
                    EventType.ENCRYPTED)
    )


    override suspend fun handleChanges(results: List<EventInsertNotification>) {
        val params = EventRelationsAggregationTask.Params(results)
        task.execute(params)
        val notificationIds = results.map { it.event_id }
        sessionDatabase.observerTriggerQueries.deleteEventInsertNotifications(notificationIds)
    }

}
