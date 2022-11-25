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
import org.matrix.android.sdk.internal.database.model.EditionOfEventFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo043(realm: DynamicRealm) : RealmMigrator(realm, 43) {

    override fun doMigrate(realm: DynamicRealm) {
        // content(string) & senderId(string) have been removed and replaced by a link to the actual event
        realm.schema.get("EditionOfEvent")
                ?.addRealmObjectField(EditionOfEventFields.EVENT.`$`, realm.schema.get("EventEntity")!!)
                ?.transform { dynamicObject ->
                    realm.where("EventEntity")
                            .equalTo(EventEntityFields.EVENT_ID, dynamicObject.getString(EditionOfEventFields.EVENT_ID))
                            .equalTo(EventEntityFields.SENDER, dynamicObject.getString("senderId"))
                            .findFirst()
                            .let {
                                dynamicObject.setObject(EditionOfEventFields.EVENT.`$`, it)
                            }
                }
                ?.removeField("senderId")
                ?.removeField("content")
    }
}
