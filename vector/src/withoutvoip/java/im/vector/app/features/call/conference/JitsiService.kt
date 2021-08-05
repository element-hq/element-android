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

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.raw.wellknown.getElementWellknown
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import javax.inject.Inject

class JitsiService @Inject constructor(
        private val session: Session,
        private val rawService: RawService,
        private val stringProvider: StringProvider) {

    suspend fun createJitsiWidget(roomId: String, withVideo: Boolean): Widget {
        // Build data for a jitsi widget
        val widgetId: String = WidgetType.Jitsi.preferred + "_" + session.myUserId + "_" + System.currentTimeMillis()
        val preferredJitsiDomain = tryOrNull {
            rawService.getElementWellknown(session.sessionParams)
                    ?.jitsiServer
                    ?.preferredDomain
        }
        val jitsiDomain = preferredJitsiDomain ?: stringProvider.getString(R.string.preferred_jitsi_domain)

        val widgetEventContent = mapOf(
                "type" to WidgetType.Jitsi.legacy,
                "data" to mapOf(
                        "domain" to jitsiDomain,
                        "isAudioOnly" to !withVideo,
                ),
                "creatorUserId" to session.myUserId,
                "id" to widgetId,
                "name" to "jitsi"
        )

        return session.widgetService().createRoomWidget(roomId, widgetId, widgetEventContent)
    }
}
