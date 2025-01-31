/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.send

/**
 * Describes a user draft:
 * REGULAR: draft of a classical message
 * QUOTE: draft of a message which quotes another message
 * EDIT: draft of an edition of a message
 * REPLY: draft of a reply of another message.
 */
sealed interface UserDraft {
    data class Regular(val content: String) : UserDraft
    data class Quote(val linkedEventId: String, val content: String) : UserDraft
    data class Edit(val linkedEventId: String, val content: String) : UserDraft
    data class Reply(val linkedEventId: String, val content: String) : UserDraft
    data class Voice(val content: String) : UserDraft

    fun isValid(): Boolean {
        return when (this) {
            is Regular -> content.isNotBlank()
            else -> true
        }
    }
}
