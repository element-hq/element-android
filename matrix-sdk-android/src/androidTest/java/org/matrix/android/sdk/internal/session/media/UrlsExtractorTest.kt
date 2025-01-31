/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

@RunWith(AndroidJUnit4::class)
internal class UrlsExtractorTest : InstrumentedTest {

    private val urlsExtractor = UrlsExtractor()

    @Test
    fun wrongEventTypeTest() {
        createEvent(body = "https://matrix.org")
                .copy(type = EventType.STATE_ROOM_GUEST_ACCESS)
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .size shouldBeEqualTo 0
    }

    @Test
    fun oneUrlTest() {
        createEvent(body = "https://matrix.org")
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .let { result ->
                    result.size shouldBeEqualTo 1
                    result[0] shouldBeEqualTo "https://matrix.org"
                }
    }

    @Test
    fun withoutProtocolTest() {
        createEvent(body = "www.matrix.org")
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .size shouldBeEqualTo 0
    }

    @Test
    fun oneUrlWithParamTest() {
        createEvent(body = "https://matrix.org?foo=bar")
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .let { result ->
                    result.size shouldBeEqualTo 1
                    result[0] shouldBeEqualTo "https://matrix.org?foo=bar"
                }
    }

    @Test
    fun oneUrlWithParamsTest() {
        createEvent(body = "https://matrix.org?foo=bar&bar=foo")
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .let { result ->
                    result.size shouldBeEqualTo 1
                    result[0] shouldBeEqualTo "https://matrix.org?foo=bar&bar=foo"
                }
    }

    @Test
    fun oneUrlInlinedTest() {
        createEvent(body = "Hello https://matrix.org, how are you?")
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .let { result ->
                    result.size shouldBeEqualTo 1
                    result[0] shouldBeEqualTo "https://matrix.org"
                }
    }

    @Test
    fun twoUrlsTest() {
        createEvent(body = "https://matrix.org https://example.org")
                .toFakeTimelineEvent()
                .let { urlsExtractor.extract(it) }
                .let { result ->
                    result.size shouldBeEqualTo 2
                    result[0] shouldBeEqualTo "https://matrix.org"
                    result[1] shouldBeEqualTo "https://example.org"
                }
    }

    private fun createEvent(body: String): Event = Event(
            eventId = "!fake",
            type = EventType.MESSAGE,
            content = MessageTextContent(
                    msgType = MessageType.MSGTYPE_TEXT,
                    body = body
            ).toContent()
    )

    private fun Event.toFakeTimelineEvent(): TimelineEvent {
        return TimelineEvent(
                root = this,
                localId = 0L,
                eventId = eventId!!,
                displayIndex = 0,
                senderInfo = SenderInfo(
                        userId = "",
                        displayName = null,
                        isUniqueDisplayName = true,
                        avatarUrl = null
                )
        )
    }
}
