/*
 * Copyright (c) 2021 New Vector Ltd
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

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject

/**
 * Decorates room summary with space related information.
 */
internal open class SpaceChildInfoEntity(
        var viaServers: RealmList<String> = RealmList(),
        // it's an active child of the space if and only if present is not null and true
        var present: Boolean? = null,
        // Use for alphabetic ordering of this child
        var order: String? = null,
        // If true, this child should be join when parent is joined
        var autoJoin: Boolean? = null,
        // link to the actual room (check type to see if it's a subspace)
        var roomSummaryEntity: RoomSummaryEntity? = null
) : RealmObject() {

    companion object
}
