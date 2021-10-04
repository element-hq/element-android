/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth

/**
 * Path to use when the client does not supported any or all login flows
 * Ref: https://matrix.org/docs/spec/client_server/latest#login-fallback
 */
internal const val LOGIN_FALLBACK_PATH = "/_matrix/static/client/login/"

/**
 * Path to use when the client does not supported any or all registration flows
 * Not documented
 */
internal const val REGISTER_FALLBACK_PATH = "/_matrix/static/client/register/"

/**
 * Path to use when the client want to connect using SSO
 * Ref: https://matrix.org/docs/spec/client_server/latest#sso-client-login
 */
internal const val SSO_REDIRECT_PATH = "/_matrix/client/r0/login/sso/redirect"

internal const val SSO_REDIRECT_URL_PARAM = "redirectUrl"

// Ref: https://matrix.org/docs/spec/client_server/r0.6.1#single-sign-on
internal const val SSO_UIA_FALLBACK_PATH = "/_matrix/client/r0/auth/m.login.sso/fallback/web"
