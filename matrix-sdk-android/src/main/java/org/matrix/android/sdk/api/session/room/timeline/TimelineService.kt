/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.timeline

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.util.Optional

/**
 * This interface defines methods to interact with the timeline. It's implemented at the room level.
 */
interface TimelineService {

    /**
     * Instantiate a [Timeline] with an optional initial eventId, to be used with permalink.
     * You can also configure some settings with the [settings] param.
     *
     * Important: the returned Timeline has to be started
     *
     * @param eventId the optional initial eventId.
     * @param settings settings to configure the timeline.
     * @return the instantiated timeline
     */
    fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline

    /**
     * Returns a snapshot of TimelineEvent event with eventId.
     * At the opposite of getTimelineEventLive which will be updated when local echo event is synced, it will return null in this case.
     * @param eventId the eventId to get the TimelineEvent
     */
    fun getTimelineEvent(eventId: String): TimelineEvent?

    /**
     * Creates a LiveData of Optional TimelineEvent event with eventId.
     * If the eventId is a local echo eventId, it will make the LiveData be updated with the synced TimelineEvent when coming through the sync.
     * In this case, makes sure to use the new synced eventId from the TimelineEvent class if you want to interact, as the local echo is removed from the SDK.
     * @param eventId the eventId to listen for TimelineEvent
     */
    fun getTimelineEventLive(eventId: String): LiveData<Optional<TimelineEvent>>

    /**
     * Returns a snapshot list of TimelineEvent with EventType.MESSAGE and MessageType.MSGTYPE_IMAGE or MessageType.MSGTYPE_VIDEO.
     */
    fun getAttachmentMessages(): List<TimelineEvent>
}
