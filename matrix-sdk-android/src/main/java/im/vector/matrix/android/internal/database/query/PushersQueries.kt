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
package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.PushRulesEntity
import im.vector.matrix.android.internal.database.model.PushRulesEntityFields
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.database.model.PusherEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun PusherEntity.Companion.where(realm: Realm,
                                          userId: String,
                                          pushKey: String? = null): RealmQuery<PusherEntity> {
    return realm.where<PusherEntity>()
            .equalTo(PusherEntityFields.USER_ID, userId)
            .apply {
                if (pushKey != null) {
                    equalTo(PusherEntityFields.PUSH_KEY, pushKey)
                }
            }
}

internal fun PushRulesEntity.Companion.where(realm: Realm,
                                             userId: String,
                                             scope: String,
                                             ruleSetKey: String): RealmQuery<PushRulesEntity> {
    return realm.where<PushRulesEntity>()
            .equalTo(PushRulesEntityFields.USER_ID, userId)
            .equalTo(PushRulesEntityFields.SCOPE, scope)
            .equalTo(PushRulesEntityFields.RULESET_KEY, ruleSetKey)
}
