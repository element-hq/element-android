/*
 * Copyright (c) 2021 New Vector Ltd
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
