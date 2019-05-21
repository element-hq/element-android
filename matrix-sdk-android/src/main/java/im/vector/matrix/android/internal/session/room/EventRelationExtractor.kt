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

import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm

/**
 * Fetches annotations (reactions, edits...) associated to a given eventEntity from the data layer.
 */
internal class EventRelationExtractor {

    fun extractFrom(event: EventEntity, realm: Realm = event.realm): EventAnnotationsSummary? {
        return EventAnnotationsSummaryEntity.where(realm, event.eventId).findFirst()?.asDomain()
    }
}