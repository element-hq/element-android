/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.threads

import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * This class contains a thread TimelineEvent along with a boolean that
 * determines if the current user has participated in that event.
 */
data class ThreadTimelineEvent(
        val timelineEvent: TimelineEvent,
        val isParticipating: Boolean
)
