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

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import org.matrix.android.sdk.internal.extensions.assertIsManaged

internal open class TimelineEventEntity(var localId: Long = 0,
                                        @Index var eventId: String = "",
                                        @Index var roomId: String = "",
                                        @Index var displayIndex: Int = 0,
                                        var root: EventEntity? = null,
                                        var annotations: EventAnnotationsSummaryEntity? = null,
                                        var senderName: String? = null,
                                        var isUniqueDisplayName: Boolean = false,
                                        var senderAvatar: String? = null,
                                        var senderMembershipEventId: String? = null,
                                        // ownedByThreadChunk indicates that the current TimelineEventEntity belongs
                                        // to a thread chunk and is a temporarily event.
                                        var ownedByThreadChunk: Boolean = false,
                                        var readReceipts: ReadReceiptsSummaryEntity? = null
) : RealmObject() {

    @LinkingObjects("timelineEvents")
    val chunk: RealmResults<ChunkEntity>? = null

    companion object
}

internal fun TimelineEventEntity.deleteOnCascade(canDeleteRoot: Boolean) {
    assertIsManaged()
    if (canDeleteRoot) {
        root?.deleteFromRealm()
    }
    deleteFromRealm()
}
