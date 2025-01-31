/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.pushrules.RuleKind
import org.matrix.android.sdk.internal.database.model.PushRuleEntity
import org.matrix.android.sdk.internal.database.model.PushRuleEntityFields
import org.matrix.android.sdk.internal.database.model.PushRulesEntity
import org.matrix.android.sdk.internal.database.model.PushRulesEntityFields
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.model.PusherEntityFields

internal fun PusherEntity.Companion.where(
        realm: Realm,
        pushKey: String? = null
): RealmQuery<PusherEntity> {
    return realm.where<PusherEntity>()
            .apply {
                if (pushKey != null) {
                    equalTo(PusherEntityFields.PUSH_KEY, pushKey)
                }
            }
}

internal fun PushRulesEntity.Companion.where(
        realm: Realm,
        scope: String,
        kind: RuleKind
): RealmQuery<PushRulesEntity> {
    return realm.where<PushRulesEntity>()
            .equalTo(PushRulesEntityFields.SCOPE, scope)
            .equalTo(PushRulesEntityFields.KIND_STR, kind.name)
}

internal fun PushRuleEntity.Companion.where(
        realm: Realm,
        scope: String,
        ruleId: String
): RealmQuery<PushRuleEntity> {
    return realm.where<PushRuleEntity>()
            .equalTo(PushRuleEntityFields.PARENT.SCOPE, scope)
            .equalTo(PushRuleEntityFields.RULE_ID, ruleId)
}
