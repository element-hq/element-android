/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.config

/**
 * The types of analytics Element currently supports.
 */
sealed interface Analytics {

    /**
     * Disables the analytics integrations.
     */
    object Disabled : Analytics

    /**
     * Analytics integration via PostHog and Sentry.
     */
    data class Enabled(
            /**
             * The PostHog instance url.
             */
            val postHogHost: String,

            /**
             * The PostHog instance API key.
             */
            val postHogApiKey: String,

            /**
             * A URL to more information about the analytics collection.
             */
            val policyLink: String,

            /**
             * The Sentry DSN url.
             */
            val sentryDSN: String,

            /**
             * Environment for Sentry.
             */
            val sentryEnvironment: String
    ) : Analytics
}
