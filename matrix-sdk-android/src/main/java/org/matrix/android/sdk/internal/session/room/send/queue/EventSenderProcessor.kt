/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.send.queue

import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Cancelable

internal interface EventSenderProcessor : SessionLifecycleObserver {

    fun postEvent(event: Event): Cancelable

    fun postEvent(event: Event, encrypt: Boolean): Cancelable

    fun postRedaction(redactionLocalEcho: Event, reason: String?): Cancelable

    fun postRedaction(redactionLocalEchoId: String, eventToRedactId: String, roomId: String, reason: String?): Cancelable

    fun postTask(task: QueuedTask): Cancelable

    fun cancel(eventId: String, roomId: String)
}
