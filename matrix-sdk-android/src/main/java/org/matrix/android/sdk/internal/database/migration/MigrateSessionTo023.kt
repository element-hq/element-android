/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo023(realm: DynamicRealm) : RealmMigrator(realm, 23) {

    override fun doMigrate(realm: DynamicRealm) {
        val eventEntity = realm.schema.get("TimelineEventEntity") ?: return

        realm.schema.get("EventEntity")
                ?.addField(EventEntityFields.IS_ROOT_THREAD, Boolean::class.java, FieldAttribute.INDEXED)
                ?.addField(EventEntityFields.ROOT_THREAD_EVENT_ID, String::class.java, FieldAttribute.INDEXED)
                ?.addField(EventEntityFields.NUMBER_OF_THREADS, Int::class.java)
                ?.addField(EventEntityFields.THREAD_NOTIFICATION_STATE_STR, String::class.java)
                ?.transform {
                    it.setString(EventEntityFields.THREAD_NOTIFICATION_STATE_STR, ThreadNotificationState.NO_NEW_MESSAGE.name)
                }
                ?.addRealmObjectField(EventEntityFields.THREAD_SUMMARY_LATEST_MESSAGE.`$`, eventEntity)
    }
}
