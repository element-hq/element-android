/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding

enum class FtueUseCase(val persistableValue: String) {
    FRIENDS_FAMILY("friends_family"),
    TEAMS("teams"),
    COMMUNITIES("communities"),
    SKIP("skip");

    companion object {
        fun from(persistedValue: String) = values().first { it.persistableValue == persistedValue }
    }
}
