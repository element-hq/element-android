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

package org.matrix.android.sdk.internal.auth

import android.net.Uri
import org.amshove.kluent.shouldBeEqualTo
import org.matrix.android.sdk.api.auth.data.DiscoveryInformation
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.internal.auth.login.LoginType
import org.matrix.android.sdk.test.fixtures.CredentialsFixture.aCredentials
import org.matrix.android.sdk.test.fixtures.DiscoveryInformationFixture.aDiscoveryInformation
import org.matrix.android.sdk.test.fixtures.HomeServerConnectionConfigFixture.aHomeServerConnectionConfig
import org.matrix.android.sdk.test.fixtures.WellKnownBaseConfigFixture.aWellKnownBaseConfig

abstract class DefaultSessionParamsCreatorTestBase {

    protected val discoveryWithHomeServer = aDiscoveryInformation(homeServer = aWellKnownBaseConfig("http://homeserver_url/"))
    private val discoveryWithIdentityServer = aDiscoveryInformation(identityServer = aWellKnownBaseConfig("http://identity_server_url/"))
    protected val credentials = aCredentials()
    protected val credentialsWithHomeServer = aCredentials(discoveryInformation = discoveryWithHomeServer)
    protected val credentialsWithIdentityServer = aCredentials(discoveryInformation = discoveryWithIdentityServer)
    protected val homeServerConnectionConfig = aHomeServerConnectionConfig()

    protected fun assertExpectedSessionParams(sessionParams: SessionParams) {
        sessionParams shouldBeEqualTo SessionParams(
                credentials = credentials,
                homeServerConnectionConfig = homeServerConnectionConfig,
                isTokenValid = true,
                loginType = LoginType.UNKNOWN,
        )
    }

    protected fun assertExpectedSessionParamsWithHomeServer(sessionParams: SessionParams) {
        sessionParams shouldBeEqualTo SessionParams(
                credentials = credentialsWithHomeServer,
                homeServerConnectionConfig = homeServerConnectionConfig.copy(homeServerUriBase = discoveryWithHomeServer.getHomeServerUri()),
                isTokenValid = true,
                loginType = LoginType.UNKNOWN,
        )
    }

    protected fun assertExpectedSessionParamsWithIdentityServer(sessionParams: SessionParams) {
        sessionParams shouldBeEqualTo SessionParams(
                credentials = credentialsWithHomeServer,
                homeServerConnectionConfig = homeServerConnectionConfig.copy(identityServerUri = discoveryWithIdentityServer.getIdentityServerUri()),
                isTokenValid = true,
                loginType = LoginType.UNKNOWN,
        )
    }

    private fun DiscoveryInformation.getIdentityServerUri() = identityServer?.baseURL?.convertToUri()!!

    protected fun DiscoveryInformation.getHomeServerUri() = homeServer?.baseURL?.convertToUri()!!

    private fun String.convertToUri() = trim { it == '/' }
            .takeIf { it.isNotBlank() }
            .let { Uri.parse(it) }
}
