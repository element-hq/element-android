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
