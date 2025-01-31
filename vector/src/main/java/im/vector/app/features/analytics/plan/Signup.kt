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
 * Triggered once onboarding has completed, but only if the user registered a
 * new account.
 */
data class Signup(
        /**
         * The type of authentication that was used to sign up.
         */
        val authenticationType: AuthenticationType,
) : VectorAnalyticsEvent {

    enum class AuthenticationType {
        /**
         * Social login using Apple.
         */
        Apple,

        /**
         * Social login using Facebook.
         */
        Facebook,

        /**
         * Social login using GitHub.
         */
        GitHub,

        /**
         * Social login using GitLab.
         */
        GitLab,

        /**
         * Social login using Google.
         */
        Google,

        /**
         * Registration using some other mechanism such as fallback.
         */
        Other,

        /**
         * Registration with a username and password.
         */
        Password,

        /**
         * Registration using another SSO provider.
         */
        SSO,
    }

    override fun getName() = "Signup"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            put("authenticationType", authenticationType.name)
        }.takeIf { it.isNotEmpty() }
    }
}
