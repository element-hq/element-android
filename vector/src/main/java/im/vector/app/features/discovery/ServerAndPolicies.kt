/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.discovery

data class ServerAndPolicies(
        val serverUrl: String,
        val policies: List<ServerPolicy>
)

data class ServerPolicy(
        val name: String,
        val url: String
)
