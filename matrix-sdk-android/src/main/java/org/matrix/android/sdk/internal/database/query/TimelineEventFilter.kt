/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

/**
 * Query strings used to filter the timeline events regarding the Json raw string of the Event.
 */
internal object TimelineEventFilter {
    /**
     * To apply to Event.content.
     */
    internal object Content {
        internal const val EDIT = """{*"m.relates_to"*"rel_type":*"m.replace"*}"""
        internal const val RESPONSE = """{*"m.relates_to"*"rel_type":*"org.matrix.response"*}"""
        internal const val REFERENCE = """{*"m.relates_to"*"rel_type":*"m.reference"*}"""
    }

    /**
     * To apply to Event.decryptionResultJson.
     */
    internal object DecryptedContent {
        internal const val URL = """{*"file":*"url":*}"""
    }

    /**
     * To apply to Event.unsigned.
     */
    internal object Unsigned {
        internal const val REDACTED = """{*"redacted_because":*}"""
    }
}
