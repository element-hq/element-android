/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import org.amshove.kluent.internal.assertFails
import org.junit.Assert.assertEquals
import org.junit.Test
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetContent
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.appendParamToUrl

private const val DOMAIN = "DOMAIN"
private const val CONF_ID = "CONF_ID"
private const val USER_ID = "USER_ID"
private const val WIDGET_ID = "WIDGET_ID"

class JitsiWidgetDataFactoryTest {

    private val jitsiWidgetDataFactory = JitsiWidgetDataFactory(DOMAIN) { widget ->
        // we don't need to compute here.
        widget.widgetContent.url
    }

    @Test
    fun jitsiWidget_V2_success() {
        val widget = createWidgetV2()
        val widgetData = jitsiWidgetDataFactory.create(widget)
        assertEquals(widgetData.confId, CONF_ID)
        assertEquals(widgetData.domain, DOMAIN)
    }

    @Test
    fun jitsiWidget_V1_success() {
        val widget = createWidgetV1(true)
        val widgetData = jitsiWidgetDataFactory.create(widget)
        assertEquals(widgetData.confId, CONF_ID)
        assertEquals(widgetData.domain, DOMAIN)
    }

    @Test
    fun jitsiWidget_V1_failure() {
        val widget = createWidgetV1(false)
        assertFails {
            jitsiWidgetDataFactory.create(widget)
        }
    }

    private fun createWidgetV1(successful: Boolean): Widget {
        val url = buildString {
            append("https://app.element.io/jitsi.html")
            if (successful) {
                appendParamToUrl("confId", CONF_ID)
            }
            append("#conferenceDomain=\$domain")
            append("&conferenceId=\$conferenceId")
            append("&isAudioOnly=\$isAudioOnly")
            append("&displayName=\$matrix_display_name")
            append("&avatarUrl=\$matrix_avatar_url")
            append("&userId=\$matrix_user_id")
            append("&roomId=\$matrix_room_id")
            append("&theme=\$theme")
        }
        val widgetEventContent = mapOf(
                "url" to url,
                "type" to WidgetType.Jitsi.preferred,
                "data" to mapOf(
                        "widgetSessionId" to WIDGET_ID
                ),
                "creatorUserId" to USER_ID,
                "id" to WIDGET_ID,
                "name" to "jitsi"
        )
        return createWidgetWithContent(widgetEventContent)
    }

    private fun createWidgetV2(): Widget {
        val widgetEventContent = mapOf(
                // We don't care of url here because we have data field
                "url" to "url",
                "type" to WidgetType.Jitsi.preferred,
                "data" to JitsiWidgetData(DOMAIN, CONF_ID, false).toContent(),
                "creatorUserId" to USER_ID,
                "id" to WIDGET_ID,
                "name" to "jitsi"
        )
        return createWidgetWithContent(widgetEventContent)
    }

    private fun createWidgetWithContent(widgetContent: Content): Widget {
        val event = Event(type = EventType.STATE_ROOM_WIDGET, eventId = "eventId", content = widgetContent)
        val widgetContentModel = widgetContent.toModel<WidgetContent>()
        return Widget(
                widgetContent = widgetContentModel!!,
                event = event,
                widgetId = WIDGET_ID,
                senderInfo = SenderInfo(USER_ID, null, false, null),
                isAddedByMe = true,
                type = WidgetType.Jitsi
        )
    }
}
