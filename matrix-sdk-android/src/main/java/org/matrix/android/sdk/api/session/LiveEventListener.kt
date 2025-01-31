/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.JsonDict

interface LiveEventListener {

    fun onLiveEvent(roomId: String, event: Event)

    fun onPaginatedEvent(roomId: String, event: Event)

    fun onEventDecrypted(event: Event, clearEvent: JsonDict)

    fun onEventDecryptionError(event: Event, throwable: Throwable)

    fun onLiveToDeviceEvent(event: Event)

    // Maybe later add more, like onJoin, onLeave..
}
