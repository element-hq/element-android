/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

class AttachmentProviderFactory @Inject constructor(
        private val imageContentRenderer: ImageContentRenderer,
        private val vectorDateFormatter: VectorDateFormatter,
        private val stringProvider: StringProvider,
        private val session: Session
) {

    fun createProvider(
            attachments: List<TimelineEvent>,
            coroutineScope: CoroutineScope
    ): RoomEventsAttachmentProvider {
        return RoomEventsAttachmentProvider(
                attachments = attachments,
                imageContentRenderer = imageContentRenderer,
                dateFormatter = vectorDateFormatter,
                fileService = session.fileService(),
                coroutineScope = coroutineScope,
                stringProvider = stringProvider
        )
    }

    fun createProvider(
            attachments: List<AttachmentData>,
            room: Room?,
            coroutineScope: CoroutineScope
    ): DataAttachmentRoomProvider {
        return DataAttachmentRoomProvider(
                attachments = attachments,
                room = room,
                imageContentRenderer = imageContentRenderer,
                dateFormatter = vectorDateFormatter,
                fileService = session.fileService(),
                coroutineScope = coroutineScope,
                stringProvider = stringProvider
        )
    }
}
