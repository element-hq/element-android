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

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.pushrules.rest.PushCondition
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
