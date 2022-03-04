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

package org.matrix.android.sdk.test.fixtures

import android.net.Uri
import okhttp3.CipherSuite
import okhttp3.TlsVersion
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.network.ssl.Fingerprint

object HomeServerConnectionConfigFixture {
    fun aHomeServerConnectionConfig(
            homeServerUri: Uri = Uri.parse("aUri"),
            homeServerUriBase: Uri = homeServerUri,
            identityServerUri: Uri? = null,
            antiVirusServerUri: Uri? = null,
            allowedFingerprints: List<Fingerprint> = emptyList(),
            shouldPin: Boolean = false,
            tlsVersions: List<TlsVersion>? = null,
            tlsCipherSuites: List<CipherSuite>? = null,
            shouldAcceptTlsExtensions: Boolean = true,
            allowHttpExtension: Boolean = false,
            forceUsageTlsVersions: Boolean = false
    ) = HomeServerConnectionConfig(
            homeServerUri,
            homeServerUriBase,
            identityServerUri,
            antiVirusServerUri,
            allowedFingerprints,
            shouldPin,
            tlsVersions,
            tlsCipherSuites,
            shouldAcceptTlsExtensions,
            allowHttpExtension,
            forceUsageTlsVersions,
    )
}
