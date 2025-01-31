/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.widgets.helper

import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.util.JsonDict

internal fun UserAccountDataEvent.extractWidgetSequence(widgetFactory: WidgetFactory): Sequence<Widget> {
    return content.asSequence()
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                (it.value as? JsonDict)?.toModel<Event>()
            }.mapNotNull { event ->
                widgetFactory.create(event)
            }
}
