/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
