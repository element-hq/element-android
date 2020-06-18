/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.legacy

import android.content.Context
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.DiscoveryInformation
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.data.WellKnownBaseConfig
import im.vector.matrix.android.api.legacy.LegacySessionImporter
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.legacy.riot.LoginStorage
import im.vector.matrix.android.internal.network.ssl.Fingerprint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class DefaultLegacySessionImporter @Inject constructor(
        context: Context,
        private val sessionParamsStore: SessionParamsStore
) : LegacySessionImporter {

    private val loginStorage = LoginStorage(context)

    override fun process() {
        Timber.d("Migration: Importing legacy session")

        val list = loginStorage.credentialsList

        Timber.d("Migration: found ${list.size} session(s).")

        val firstConfig = list.firstOrNull() ?: return

        Timber.d("Migration: importing a session")

        @Suppress("DEPRECATION")
        val sessionParams = SessionParams(
                credentials = Credentials(
                        userId = firstConfig.credentials.userId,
                        accessToken = firstConfig.credentials.accessToken,
                        refreshToken = firstConfig.credentials.refreshToken,
                        homeServer = firstConfig.credentials.homeServer,
                        deviceId = firstConfig.credentials.deviceId,
                        discoveryInformation = firstConfig.credentials.wellKnown?.let { wellKnown ->
                            // Note credentials.wellKnown is not serialized in the LoginStorage, so this code is a bit useless...
                            if (wellKnown.homeServer?.baseURL != null
                                    || wellKnown.identityServer?.baseURL != null) {
                                DiscoveryInformation(
                                        homeServer = wellKnown.homeServer?.baseURL?.let { WellKnownBaseConfig(baseURL = it) },
                                        identityServer = wellKnown.identityServer?.baseURL?.let { WellKnownBaseConfig(baseURL = it) }
                                )
                            } else {
                                null
                            }
                        }
                ),
                homeServerConnectionConfig = HomeServerConnectionConfig(
                        homeServerUri = firstConfig.homeserverUri,
                        identityServerUri = firstConfig.identityServerUri,
                        antiVirusServerUri = firstConfig.antiVirusServerUri,
                        allowedFingerprints = firstConfig.allowedFingerprints.map {
                            Fingerprint(
                                    bytes = it.bytes,
                                    hashType = when (it.type) {
                                        im.vector.matrix.android.internal.legacy.riot.Fingerprint.HashType.SHA1,
                                        null                                                                      -> Fingerprint.HashType.SHA1
                                        im.vector.matrix.android.internal.legacy.riot.Fingerprint.HashType.SHA256 -> Fingerprint.HashType.SHA256
                                    }
                            )
                        },
                        shouldPin = firstConfig.shouldPin(),
                        tlsVersions = firstConfig.acceptedTlsVersions,
                        tlsCipherSuites = firstConfig.acceptedTlsCipherSuites,
                        shouldAcceptTlsExtensions = firstConfig.shouldAcceptTlsExtensions(),
                        allowHttpExtension = false, // TODO
                        forceUsageTlsVersions = firstConfig.forceUsageOfTlsVersions()
                ),
                // If token is not valid, this boolean will be updated later
                isTokenValid = true
        )

        Timber.d("Migration: save session")

        GlobalScope.launch {
            sessionParamsStore.save(sessionParams)
        }

        Timber.d("Migration: clear legacy session")

        // Delete to avoid doing this several times
        loginStorage.clear()

        // TODO Crypto DB

        // TODO Remove
        Thread.sleep(5000)
    }
}
