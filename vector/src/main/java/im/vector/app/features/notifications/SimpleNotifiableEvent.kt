/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

data class SimpleNotifiableEvent(
        val matrixID: String?,
        override val eventId: String,
        override val editedEventId: String?,
        val noisy: Boolean,
        val title: String,
        val description: String,
        val type: String?,
        val timestamp: Long,
        val soundName: String?,
        override var canBeReplaced: Boolean,
        override val isRedacted: Boolean = false,
        override val isUpdated: Boolean = false
) : NotifiableEvent
