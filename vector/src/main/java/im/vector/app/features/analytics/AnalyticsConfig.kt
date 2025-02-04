/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics

data class AnalyticsConfig(
        val isEnabled: Boolean,
        val postHogHost: String,
        val postHogApiKey: String,
        val policyLink: String,
        val sentryDSN: String,
        val sentryEnvironment: String
)
