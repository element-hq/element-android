/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationShareAggregatedSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconLocationDataContent
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import javax.inject.Inject

internal class LiveLocationShareAggregatedSummaryMapper @Inject constructor() :
        Monarchy.Mapper<LiveLocationShareAggregatedSummary, LiveLocationShareAggregatedSummaryEntity> {

    override fun map(entity: LiveLocationShareAggregatedSummaryEntity): LiveLocationShareAggregatedSummary {
        return LiveLocationShareAggregatedSummary(
                userId = entity.userId,
                isActive = entity.isActive,
                endOfLiveTimestampMillis = entity.endOfLiveTimestampMillis,
                lastLocationDataContent = ContentMapper.map(entity.lastLocationContent).toModel<MessageBeaconLocationDataContent>()
        )
    }
}
