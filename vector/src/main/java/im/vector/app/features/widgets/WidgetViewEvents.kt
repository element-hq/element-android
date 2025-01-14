/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
