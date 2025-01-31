/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.relation

import org.matrix.android.sdk.api.session.events.model.RelationType

interface RelationContent {
    /** See [RelationType] for known possible values. */
    val type: String?
    val eventId: String?
    val inReplyTo: ReplyToContent?
    val option: Int?

    /**
     * This flag indicates that the message should be rendered as a reply
     * fallback, when isFallingBack = false.
     */
    val isFallingBack: Boolean?
}
