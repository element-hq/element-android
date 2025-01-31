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
 * Triggered when the user joins a room.
 */
data class JoinedRoom(
        /**
         * Whether the room is a DM.
         */
        val isDM: Boolean,
        /**
         * Whether the room is a Space.
         */
        val isSpace: Boolean,
        /**
         * The size of the room.
         */
        val roomSize: RoomSize,
        /**
         * The trigger for a room being joined if known.
         */
        val trigger: Trigger? = null,
) : VectorAnalyticsEvent {

    enum class Trigger {
        /**
         * Room joined via an invite.
         */
        Invite,

        /**
         * Room joined via link.
         */
        MobilePermalink,

        /**
         * Room joined via a push/desktop notification.
         */
        Notification,

        /**
         * Room joined via the public rooms directory.
         */
        RoomDirectory,

        /**
         * Room joined via its preview.
         */
        RoomPreview,

        /**
         * Room joined via the /join slash command.
         */
        SlashCommand,

        /**
         * Room joined via the space hierarchy view.
         */
        SpaceHierarchy,

        /**
         * Room joined via a timeline pill or link in another room.
         */
        Timeline,
    }

    enum class RoomSize {
        ElevenToOneHundred,
        MoreThanAThousand,
        One,
        OneHundredAndOneToAThousand,
        ThreeToTen,
        Two,
    }

    override fun getName() = "JoinedRoom"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("isDM", isDM)
            put("isSpace", isSpace)
            put("roomSize", roomSize.name)
            trigger?.let { put("trigger", it.name) }
        }.takeIf { it.isNotEmpty() }
    }
}
