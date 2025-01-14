/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

import java.io.Serializable

/**
 * Parent interface for all events which can be displayed as a Notification.
 */
sealed interface NotifiableEvent : Serializable {
    val eventId: String
    val editedEventId: String?

    // Used to know if event should be replaced with the one coming from eventstream
    val canBeReplaced: Boolean
    val isRedacted: Boolean
    val isUpdated: Boolean
}
