/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.widgets.model.Widget
import java.net.URL
import java.net.URLDecoder

class JitsiWidgetDataFactory(private val fallbackJitsiDomain: String, private val urlComputer: (Widget) -> String?) {

    /**
     * Extract JitsiWidgetData from a widget.
     * For Widget V2, it will extract data from content.data
     * For Widget V1, it will extract data from url.
     */
    fun create(widget: Widget): JitsiWidgetData {
        return widget.widgetContent.data.toModel<JitsiWidgetData>() ?: widget.createFromUrl()
    }

    /**
     * This creates a JitsiWidgetData from the url.
     * It's a fallback for Widget V1.
     * It first get the computed url and then tries to extract JitsiWidgetData from it.
     */
    private fun Widget.createFromUrl(): JitsiWidgetData {
        return urlComputer(this)?.let { url -> createFromUrl(url) } ?: throw IllegalStateException()
    }

    private fun createFromUrl(url: String): JitsiWidgetData {
        val configString = tryOrNull { URL(url) }?.query
        val configs = configString?.split("&")
                ?.map { it.split("=") }
                ?.filter { it.size == 2 }
                ?.map { (key, value) -> key to URLDecoder.decode(value, "UTF-8") }
                ?.toMap()
                .orEmpty()

        return JitsiWidgetData(
                domain = configs["conferenceDomain"] ?: fallbackJitsiDomain,
                confId = configs["conferenceId"] ?: configs["confId"] ?: throw IllegalStateException(),
                isAudioOnly = configs["isAudioOnly"].toBoolean(),
                auth = configs["auth"]
        )
    }
}
