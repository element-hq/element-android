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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import javax.inject.Inject

/**
 * Acts as a listener of incoming messages in order to incrementally computes a summary of annotations.
 * For reactions will build a EventAnnotationsSummaryEntity, ans for edits a EditAggregatedSummaryEntity.
 * The summaries can then be extracted and added (as a decoration) to a TimelineEvent for final display.
 */

internal class EventRelationsAggregationUpdater @Inject constructor(monarchy: Monarchy,
                                                                    private val credentials: Credentials,
                                                                    private val task: EventRelationsAggregationTask,
                                                                    private val taskExecutor: TaskExecutor) :
        RealmLiveEntityObserver<EventEntity>(monarchy) {

    override val query = Monarchy.Query<EventEntity> {
        EventEntity.where(it)
        //mmm why is this query not working?
//        EventEntity.byTypes(it, listOf(
//                EventType.REDACTION, EventType.MESSAGE, EventType.REDACTION)
//        )
    }

    override fun processChanges(inserted: List<EventEntity>, updated: List<EventEntity>, deleted: List<EventEntity>) {
        Timber.v("EventRelationsAggregationUpdater called with ${inserted.size} insertions")
        val domainInserted = inserted
                .map { it.asDomain() to it.sendState }

        val params = EventRelationsAggregationTask.Params(
                domainInserted,
                credentials.userId
        )

        task.configureWith(params)
                .executeBy(taskExecutor)

    }

}

