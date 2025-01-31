/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when the user runs a slash command in their composer.
 */
data class SlashCommand(
        /**
         * The name of this command.
         */
        val command: Command,
) : VectorAnalyticsEvent {

    enum class Command {
        Invite,
        Part,
    }

    override fun getName() = "SlashCommand"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("command", command.name)
        }.takeIf { it.isNotEmpty() }
    }
}
