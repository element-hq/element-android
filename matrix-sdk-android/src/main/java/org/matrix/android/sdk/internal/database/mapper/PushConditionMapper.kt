/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.pushrules.rest.PushCondition
import org.matrix.android.sdk.internal.database.model.PushConditionEntity

internal object PushConditionMapper {

    fun map(entity: PushConditionEntity): PushCondition {
        return PushCondition(
                kind = entity.kind,
                iz = entity.iz,
                key = entity.key,
                pattern = entity.pattern
        )
    }

    fun map(domain: PushCondition): PushConditionEntity {
        return PushConditionEntity(
                kind = domain.kind,
                iz = domain.iz,
                key = domain.key,
                pattern = domain.pattern
        )
    }
}
