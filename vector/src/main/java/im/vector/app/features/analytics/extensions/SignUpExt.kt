/*
 * Copyright (c) 2022 New Vector Ltd
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
