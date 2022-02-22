/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.model.threads

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity

internal open class ThreadSummaryEntity(@Index var rootThreadEventId: String? = "",
                                        var rootThreadEventEntity: EventEntity? = null,
                                        var latestThreadEventEntity: EventEntity? = null,
                                        var rootThreadSenderName: String? = null,
                                        var latestThreadSenderName: String? = null,
                                        var rootThreadSenderAvatar: String? = null,
                                        var latestThreadSenderAvatar: String? = null,
                                        var rootThreadIsUniqueDisplayName: Boolean = false,
                                        var isUserParticipating: Boolean = false,
                                        var latestThreadIsUniqueDisplayName: Boolean = false,
                                        var numberOfThreads: Int = 0
) : RealmObject() {

    @LinkingObjects("threadSummaries")
    val room: RealmResults<RoomEntity>? = null

    companion object
}
