/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
