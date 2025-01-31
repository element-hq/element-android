/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.widgets.model

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

data class Widget(
        val widgetContent: WidgetContent,
        val event: Event,
        val widgetId: String,
        val senderInfo: SenderInfo?,
        val isAddedByMe: Boolean,
        val type: WidgetType
) {

    val isActive = widgetContent.isActive()

    val name = widgetContent.getHumanName()
}
