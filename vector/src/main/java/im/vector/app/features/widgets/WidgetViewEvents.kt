/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.widgets

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.session.events.model.Content

sealed class WidgetViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : WidgetViewEvents()
    data class Close(val content: Content? = null) : WidgetViewEvents()
    data class DisplayIntegrationManager(val integId: String?, val integType: String?) : WidgetViewEvents()
    data class OnURLFormatted(val formattedURL: String) : WidgetViewEvents()
    data class DisplayTerms(val url: String, val token: String) : WidgetViewEvents()
}
