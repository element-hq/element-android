/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.model.accountdata

/**
 * Keys are userIds, values are list of roomIds.
 */
internal typealias DirectMessagesContent = Map<String, List<String>>

/**
 * Returns a new [MutableMap] with all elements of this collection.
 */
internal fun DirectMessagesContent.toMutable(): MutableMap<String, MutableList<String>> {
    return map { it.key to it.value.toMutableList() }
            .toMap()
            .toMutableMap()
}
