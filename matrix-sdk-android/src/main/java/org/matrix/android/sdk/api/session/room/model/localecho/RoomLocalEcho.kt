/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.localecho

import java.util.UUID

object RoomLocalEcho {

    const val PREFIX = "!local."

    /**
     * Tell whether the provider room id is a local id.
     */
    fun isLocalEchoId(roomId: String) = roomId.startsWith(PREFIX)

    internal fun createLocalEchoId() = "${PREFIX}${UUID.randomUUID()}"
}
