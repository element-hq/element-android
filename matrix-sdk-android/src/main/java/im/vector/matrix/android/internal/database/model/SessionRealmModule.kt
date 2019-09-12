/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.database.model

import io.realm.annotations.RealmModule

/**
 * Realm module for Session
 */
@RealmModule(library = true,
             classes = [
                 ChunkEntity::class,
                 EventEntity::class,
                 TimelineEventEntity::class,
                 FilterEntity::class,
                 GroupEntity::class,
                 GroupSummaryEntity::class,
                 ReadReceiptEntity::class,
                 RoomEntity::class,
                 RoomSummaryEntity::class,
                 RoomTagEntity::class,
                 SyncEntity::class,
                 UserEntity::class,
                 EventAnnotationsSummaryEntity::class,
                 ReactionAggregatedSummaryEntity::class,
                 EditAggregatedSummaryEntity::class,
                 PushRulesEntity::class,
                 PushRuleEntity::class,
                 PushConditionEntity::class,
                 PusherEntity::class,
                 PusherDataEntity::class,
                 ReadReceiptsSummaryEntity::class,
                 UserDraftsEntity::class,
                 DraftEntity::class
             ])
internal class SessionRealmModule
