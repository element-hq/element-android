/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.alias

interface AliasService {
    /**
     * Get list of local alias of the room.
     * @return the list of the aliases (full aliases, not only the local part)
     */
    suspend fun getRoomAliases(): List<String>

    /**
     * Add local alias to the room.
     * @param aliasLocalPart the local part of the alias.
     * Ex: for the alias "#my_alias:example.org", the local part is "my_alias"
     */
    suspend fun addAlias(aliasLocalPart: String)
}
