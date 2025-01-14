/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.Signup
import im.vector.app.features.onboarding.AuthenticationDescription

fun AuthenticationDescription.AuthenticationType.toAnalyticsType() = when (this) {
    AuthenticationDescription.AuthenticationType.Password -> Signup.AuthenticationType.Password
    AuthenticationDescription.AuthenticationType.Apple -> Signup.AuthenticationType.Apple
    AuthenticationDescription.AuthenticationType.Facebook -> Signup.AuthenticationType.Facebook
    AuthenticationDescription.AuthenticationType.GitHub -> Signup.AuthenticationType.GitHub
    AuthenticationDescription.AuthenticationType.GitLab -> Signup.AuthenticationType.GitLab
    AuthenticationDescription.AuthenticationType.Google -> Signup.AuthenticationType.Google
    AuthenticationDescription.AuthenticationType.SSO -> Signup.AuthenticationType.SSO
    AuthenticationDescription.AuthenticationType.Other -> Signup.AuthenticationType.Other
}
