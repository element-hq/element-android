/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.analytics.AnalyticsConfig

object AnalyticsConfigFixture {
    fun anAnalyticsConfig(
            isEnabled: Boolean = false,
            postHogHost: String = "http://posthog.url",
            postHogApiKey: String = "api-key",
            policyLink: String = "http://policy.link",
            sentryDSN: String = "http://sentry.dsn",
            sentryEnvironment: String = "sentry-env"
    ) = AnalyticsConfig(isEnabled, postHogHost, postHogApiKey, policyLink, sentryDSN, sentryEnvironment)
}
