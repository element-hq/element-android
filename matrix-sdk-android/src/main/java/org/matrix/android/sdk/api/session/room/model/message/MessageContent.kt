/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

interface MessageContent {

    companion object {
        const val MSG_TYPE_JSON_KEY = "msgtype"
    }

    val msgType: String
    val body: String
    val relatesTo: RelationDefaultContent?
    val newContent: Content?
}
