/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.session.events.model

/**
 * Constants defining known event relation types from Matrix specifications.
 */
object RelationType {
    /** Lets you define an event which annotates an existing event.*/
    const val ANNOTATION = "m.annotation"

    /** Lets you define an event which replaces an existing event.*/
    const val REPLACE = "m.replace"

    /** Lets you define an event which references an existing event.*/
    const val REFERENCE = "m.reference"

    /** Lets you define an event which is a thread reply to an existing event.*/
    const val THREAD = "m.thread"

    /** Lets you define an event which adds a response to an existing event.*/
    const val RESPONSE = "org.matrix.response"
}
