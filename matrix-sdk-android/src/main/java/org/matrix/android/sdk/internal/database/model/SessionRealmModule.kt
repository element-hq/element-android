/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.annotations.RealmModule
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity

/**
 * Realm module for Session.
 */
@RealmModule(
        library = true,
        classes = [
            ChunkEntity::class,
            EventEntity::class,
            EventInsertEntity::class,
            TimelineEventEntity::class,
            FilterEntity::class,
            ReadReceiptEntity::class,
            RoomEntity::class,
            RoomSummaryEntity::class,
            LocalRoomSummaryEntity::class,
            RoomTagEntity::class,
            SyncEntity::class,
            PendingThreePidEntity::class,
            UserEntity::class,
            IgnoredUserEntity::class,
            BreadcrumbsEntity::class,
            UserThreePidEntity::class,
            EventAnnotationsSummaryEntity::class,
            ReactionAggregatedSummaryEntity::class,
            EditAggregatedSummaryEntity::class,
            EditionOfEvent::class,
            PollResponseAggregatedSummaryEntity::class,
            LiveLocationShareAggregatedSummaryEntity::class,
            ReferencesAggregatedSummaryEntity::class,
            PushRulesEntity::class,
            PushRuleEntity::class,
            PushConditionEntity::class,
            PreviewUrlCacheEntity::class,
            PusherEntity::class,
            PusherDataEntity::class,
            ReadReceiptsSummaryEntity::class,
            ReadMarkerEntity::class,
            UserDraftsEntity::class,
            DraftEntity::class,
            HomeServerCapabilitiesEntity::class,
            RoomMemberSummaryEntity::class,
            CurrentStateEventEntity::class,
            UserAccountDataEntity::class,
            ScalarTokenEntity::class,
            WellknownIntegrationManagerConfigEntity::class,
            RoomAccountDataEntity::class,
            SpaceChildSummaryEntity::class,
            SpaceParentSummaryEntity::class,
            UserPresenceEntity::class,
            ThreadSummaryEntity::class
        ]
)
internal class SessionRealmModule
