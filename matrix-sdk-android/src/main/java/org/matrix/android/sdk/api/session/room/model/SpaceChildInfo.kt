/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

data class SpaceChildInfo(
        val childRoomId: String,
        // We might not know this child at all,
        // i.e we just know it exists but no info on type/name/etc..
        val isKnown: Boolean,
        val roomType: String?,
        val name: String?,
        val topic: String?,
        val avatarUrl: String?,
        val order: String?,
        val activeMemberCount: Int?,
//        val autoJoin: Boolean,
        val viaServers: List<String>,
        val parentRoomId: String?,
        val suggested: Boolean?,
        val canonicalAlias: String?,
        val aliases: List<String>?,
        val worldReadable: Boolean
)
