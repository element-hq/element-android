/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.events.model

import java.util.UUID

object LocalEcho {

    private const val PREFIX = "\$local."

    fun isLocalEchoId(eventId: String) = eventId.startsWith(PREFIX)

    fun createLocalEchoId() = "${PREFIX}${UUID.randomUUID()}"
}
