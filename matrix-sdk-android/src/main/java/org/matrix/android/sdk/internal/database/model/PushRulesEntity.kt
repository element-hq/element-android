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
package org.matrix.android.sdk.internal.database.model

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.internal.database.clearWith

internal class PushRulesEntity : RealmObject {
    var scope: String = ""
    var pushRules: RealmList<PushRuleEntity> = realmListOf()

    private var kindStr: String = RuleKind.CONTENT.name
    var kind: RuleKind
        get() {
            return RuleKind.valueOf(kindStr)
        }
        set(value) {
            kindStr = value.name
        }

    companion object
}

internal fun MutableRealm.deleteOnCascade(pushRulesEntity: PushRulesEntity) {
    pushRulesEntity.pushRules.clearWith { deleteOnCascade(it) }
    delete(pushRulesEntity)
}
