/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

data class InviteNotifiableEvent(
        val matrixID: String?,
        override val eventId: String,
        override val editedEventId: String?,
        override val canBeReplaced: Boolean,
        val roomId: String,
        val roomName: String?,
        val noisy: Boolean,
        val title: String,
        val description: String,
        val type: String?,
        val timestamp: Long,
        val soundName: String?,
        override val isRedacted: Boolean = false,
        override val isUpdated: Boolean = false
) : NotifiableEvent
