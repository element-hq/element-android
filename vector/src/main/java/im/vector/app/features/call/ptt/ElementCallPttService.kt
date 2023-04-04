/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.call.ptt

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.time.Clock
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.appendParamToUrl
import javax.inject.Inject

class ElementCallPttService @Inject constructor(
        private val session: Session,
        private val stringProvider: StringProvider,
        private val clock: Clock,
) {

    suspend fun createElementCallPttWidget(roomId: String, roomAlias: String): Widget {
        val widgetId = WidgetType.ElementCall.preferred + "_" + session.myUserId + "_" + clock.epochMillis()
        val elementCallDomain = stringProvider.getString(R.string.preferred_element_call_domain)

        val url = buildString {
            append(elementCallDomain)
            appendParamToUrl("enableE2e", "false")
            append("&ptt=true")
            append("&displayName=\$matrix_display_name")
            append(roomAlias)
        }

        val widgetEventContent = mapOf(
                "url" to url,
                "type" to WidgetType.ElementCall.legacy,
                "id" to widgetId
        )

        return session.widgetService().createRoomWidget(roomId, widgetId, widgetEventContent)
    }
}
