/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import io.realm.RealmQuery
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.filterEvents

internal fun RealmQuery<TimelineEventEntity>.filterEventsWithSettings(settings: TimelineSettings): RealmQuery<TimelineEventEntity> {
    return filterEvents(settings.filters)
}

internal fun List<TimelineEvent>.filterEventsWithSettings(settings: TimelineSettings): List<TimelineEvent> {
    return filter { event ->
        val filterType = !settings.filters.filterTypes
                || settings.filters.allowedTypes.any { it.eventType == event.root.type && (it.stateKey == null || it.stateKey == event.root.senderId) }
        if (!filterType) return@filter false

        val filterEdits = if (settings.filters.filterEdits && event.root.getClearType() == EventType.MESSAGE) {
            val messageContent = event.root.getClearContent().toModel<MessageContent>()
            messageContent?.relatesTo?.type != RelationType.REPLACE && messageContent?.relatesTo?.type != RelationType.RESPONSE
        } else {
            true
        }
        if (!filterEdits) return@filter false

        val filterRedacted = settings.filters.filterRedacted && event.root.isRedacted()
        !filterRedacted
    }
}
