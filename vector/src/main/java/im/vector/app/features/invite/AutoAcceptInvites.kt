/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.invite

import javax.inject.Inject

/**
 * This interface defines 2 flags so you can handle auto accept invites.
 * At the moment we only have [CompileTimeAutoAcceptInvites] implementation.
 */
interface AutoAcceptInvites {
    /**
     * Enable auto-accept invites. It means, as soon as you got an invite from the sync, it will try to join it.
     */
    val isEnabled: Boolean

    /**
     * Hide invites from the UI (from notifications, notification count and room list). By default invites are hidden when [isEnabled] is true
     */
    val hideInvites: Boolean
        get() = isEnabled
}

fun AutoAcceptInvites.showInvites() = !hideInvites

/**
 * Simple compile time implementation of AutoAcceptInvites flags.
 */
class CompileTimeAutoAcceptInvites @Inject constructor() : AutoAcceptInvites {
    override val isEnabled = false
}
