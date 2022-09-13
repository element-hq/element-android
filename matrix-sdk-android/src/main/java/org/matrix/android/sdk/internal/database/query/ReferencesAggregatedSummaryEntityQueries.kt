/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.ReferencesAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.ReferencesAggregatedSummaryEntityFields

internal fun ReferencesAggregatedSummaryEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<ReferencesAggregatedSummaryEntity> {
    val query = realm.where<ReferencesAggregatedSummaryEntity>()
    query.equalTo(ReferencesAggregatedSummaryEntityFields.EVENT_ID, eventId)
    return query
}

internal fun ReferencesAggregatedSummaryEntity.Companion.create(realm: Realm, txID: String): ReferencesAggregatedSummaryEntity {
    return realm.createObject(ReferencesAggregatedSummaryEntity::class.java).apply {
        this.eventId = txID
    }
}
