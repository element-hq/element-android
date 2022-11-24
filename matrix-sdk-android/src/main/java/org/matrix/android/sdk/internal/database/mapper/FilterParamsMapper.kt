/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import io.realm.RealmList
import org.matrix.android.sdk.internal.database.model.SyncFilterParamsEntity
import org.matrix.android.sdk.internal.sync.filter.SyncFilterParams
import javax.inject.Inject

internal class FilterParamsMapper @Inject constructor() {

    fun map(entity: SyncFilterParamsEntity): SyncFilterParams {
        val eventTypes = if (entity.listOfSupportedEventTypesHasBeenSet) {
            entity.listOfSupportedEventTypes?.toList()
        } else {
            null
        }
        val stateEventTypes = if (entity.listOfSupportedStateEventTypesHasBeenSet) {
            entity.listOfSupportedStateEventTypes?.toList()
        } else {
            null
        }
        return SyncFilterParams(
                useThreadNotifications = entity.useThreadNotifications,
                lazyLoadMembersForMessageEvents = entity.lazyLoadMembersForMessageEvents,
                lazyLoadMembersForStateEvents = entity.lazyLoadMembersForStateEvents,
                listOfSupportedEventTypes = eventTypes,
                listOfSupportedStateEventTypes = stateEventTypes,
        )
    }

    fun map(params: SyncFilterParams): SyncFilterParamsEntity {
        return SyncFilterParamsEntity(
                useThreadNotifications = params.useThreadNotifications,
                lazyLoadMembersForMessageEvents = params.lazyLoadMembersForMessageEvents,
                lazyLoadMembersForStateEvents = params.lazyLoadMembersForStateEvents,
                listOfSupportedEventTypes = params.listOfSupportedEventTypes.toRealmList(),
                listOfSupportedEventTypesHasBeenSet = params.listOfSupportedEventTypes != null,
                listOfSupportedStateEventTypes = params.listOfSupportedStateEventTypes.toRealmList(),
                listOfSupportedStateEventTypesHasBeenSet = params.listOfSupportedStateEventTypes != null,
        )
    }

    private fun List<String>?.toRealmList(): RealmList<String>? {
        return this?.toTypedArray()?.let { RealmList(*it) }
    }
}
