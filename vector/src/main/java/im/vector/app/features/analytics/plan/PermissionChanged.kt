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
 * Triggered when the user changes a permission status.
 */
data class PermissionChanged(
        /**
         * Whether the permission has been granted by the user.
         */
        val granted: Boolean,
        /**
         * The name of the permission.
         */
        val permission: Permission,
) : VectorAnalyticsEvent {

    enum class Permission {
        /**
         * Permissions related to sending notifications have changed.
         */
        Notification,
    }

    override fun getName() = "PermissionChanged"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("granted", granted)
            put("permission", permission.name)
        }.takeIf { it.isNotEmpty() }
    }
}
