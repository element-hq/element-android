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
import im.vector.matrix.android.internal.legacy.riot.Fingerprint as LegacyFingerprint
import im.vector.matrix.android.internal.legacy.riot.HomeServerConnectionConfig as LegacyHomeServerConnectionConfig

internal class DefaultLegacySessionImporter @Inject constructor(
        context: Context,
        private val sessionParamsStore: SessionParamsStore
) : LegacySessionImporter {

    private val loginStorage = LoginStorage(context)

    override fun process() {
        Timber.d("Migration: Importing legacy session")

        val list = loginStorage.credentialsList

        Timber.d("Migration: found ${list.size} session(s).")

        val legacyConfig = list.firstOrNull() ?: return

        GlobalScope.launch {
            Timber.d("Migration: importing a session")
            importCredentials(legacyConfig)

            Timber.d("Migration: importing crypto DB")
            importCryptoDb(legacyConfig)

            Timber.d("Migration: clear legacy session")

            // Delete to avoid doing this several times
            loginStorage.clear()
        }
    }

    private suspend fun importCredentials(legacyConfig: LegacyHomeServerConnectionConfig) {
        @Suppress("DEPRECATION")
        val sessionParams = SessionParams(
                credentials = Credentials(
                        userId = legacyConfig.credentials.userId,
                        accessToken = legacyConfig.credentials.accessToken,
                        refreshToken = legacyConfig.credentials.refreshToken,
                        homeServer = legacyConfig.credentials.homeServer,
                        deviceId = legacyConfig.credentials.deviceId,
                        discoveryInformation = legacyConfig.credentials.wellKnown?.let { wellKnown ->
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
                        homeServerUri = legacyConfig.homeserverUri,
                        identityServerUri = legacyConfig.identityServerUri,
                        antiVirusServerUri = legacyConfig.antiVirusServerUri,
                        allowedFingerprints = legacyConfig.allowedFingerprints.map {
                            Fingerprint(
                                    bytes = it.bytes,
                                    hashType = when (it.type) {
                                        LegacyFingerprint.HashType.SHA1,
                                        null                              -> Fingerprint.HashType.SHA1
                                        LegacyFingerprint.HashType.SHA256 -> Fingerprint.HashType.SHA256
                                    }
                            )
                        },
                        shouldPin = legacyConfig.shouldPin(),
                        tlsVersions = legacyConfig.acceptedTlsVersions,
                        tlsCipherSuites = legacyConfig.acceptedTlsCipherSuites,
                        shouldAcceptTlsExtensions = legacyConfig.shouldAcceptTlsExtensions(),
                        allowHttpExtension = false, // TODO
                        forceUsageTlsVersions = legacyConfig.forceUsageOfTlsVersions()
                ),
                // If token is not valid, this boolean will be updated later
                isTokenValid = true
        )

        Timber.d("Migration: save session")
        sessionParamsStore.save(sessionParams)
    }

    private suspend fun importCryptoDb(legacyConfig: LegacyHomeServerConnectionConfig) {
        TODO("Not yet implemented")
    }
}
