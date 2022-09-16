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

import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmSingleQuery
import org.matrix.android.sdk.internal.database.model.ReadReceiptsSummaryEntity

internal fun ReadReceiptsSummaryEntity.Companion.where(realm: TypedRealm, eventId: String): RealmSingleQuery<ReadReceiptsSummaryEntity> {
    return realm.query(ReadReceiptsSummaryEntity::class)
            .query("eventId == $0", eventId)
            .first()
}

internal fun ReadReceiptsSummaryEntity.Companion.whereInRoom(realm: TypedRealm, roomId: String): RealmQuery<ReadReceiptsSummaryEntity> {
    return realm.query(ReadReceiptsSummaryEntity::class)
            .query("roomId == $0", roomId)
}
