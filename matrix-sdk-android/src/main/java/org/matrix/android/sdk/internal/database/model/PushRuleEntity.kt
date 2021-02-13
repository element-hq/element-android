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

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

internal open class PushRuleEntity(
        // Required. The actions to perform when this rule is matched.
        var actionsStr: String? = null,
        // Required. Whether this is a default rule, or has been set explicitly.
        var default: Boolean = false,
        // Required. Whether the push rule is enabled or not.
        var enabled: Boolean = true,
        // Required. The ID of this rule.
        var ruleId: String = "",
        // The conditions that must hold true for an event in order for a rule to be applied to an event
        var conditions: RealmList<PushConditionEntity>? = RealmList(),
        // The glob-style pattern to match against. Only applicable to content rules.
        var pattern: String? = null
) : RealmObject() {

    @LinkingObjects("pushRules")
    val parent: RealmResults<PushRulesEntity>? = null

    companion object
}

internal fun PushRuleEntity.deleteOnCascade() {
    conditions?.deleteAllFromRealm()
    deleteFromRealm()
}
