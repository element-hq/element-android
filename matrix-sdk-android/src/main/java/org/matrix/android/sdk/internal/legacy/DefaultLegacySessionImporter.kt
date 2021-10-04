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

package org.matrix.android.sdk.internal.legacy

import android.content.Context
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.DiscoveryInformation
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.auth.data.WellKnownBaseConfig
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreMigration
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.legacy.riot.LoginStorage
import org.matrix.android.sdk.internal.network.ssl.Fingerprint
import org.matrix.android.sdk.internal.util.md5
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import org.matrix.android.sdk.internal.legacy.riot.Fingerprint as LegacyFingerprint
import org.matrix.android.sdk.internal.legacy.riot.HomeServerConnectionConfig as LegacyHomeServerConnectionConfig

internal class DefaultLegacySessionImporter @Inject constructor(
        private val context: Context,
        private val sessionParamsStore: SessionParamsStore,
        private val realmKeysUtils: RealmKeysUtils
) : LegacySessionImporter {

    private val loginStorage = LoginStorage(context)

    companion object {
        // During development, set to false to play several times the migration
        private var DELETE_PREVIOUS_DATA = true
    }

    override fun process(): Boolean {
        Timber.d("Migration: Importing legacy session")

        val list = loginStorage.credentialsList

        Timber.d("Migration: found ${list.size} session(s).")

        val legacyConfig = list.firstOrNull() ?: return false

        runBlocking {
            Timber.d("Migration: importing a session")
            try {
                importCredentials(legacyConfig)
            } catch (t: Throwable) {
                // It can happen in case of partial migration. To test, do not return
                Timber.e(t, "Migration: Error importing credential")
            }

            Timber.d("Migration: importing crypto DB")
            try {
                importCryptoDb(legacyConfig)
            } catch (t: Throwable) {
                // It can happen in case of partial migration. To test, do not return
                Timber.e(t, "Migration: Error importing crypto DB")
            }

            if (DELETE_PREVIOUS_DATA) {
                try {
                    Timber.d("Migration: clear file system")
                    clearFileSystem(legacyConfig)
                } catch (t: Throwable) {
                    Timber.e(t, "Migration: Error clearing filesystem")
                }
                try {
                    Timber.d("Migration: clear shared prefs")
                    clearSharedPrefs()
                } catch (t: Throwable) {
                    Timber.e(t, "Migration: Error clearing shared prefs")
                }
            } else {
                Timber.d("Migration: clear file system - DEACTIVATED")
                Timber.d("Migration: clear shared prefs - DEACTIVATED")
            }
        }

        // A session has been imported
        return true
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
                            if (wellKnown.homeServer?.baseURL != null || wellKnown.identityServer?.baseURL != null) {
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

    private fun importCryptoDb(legacyConfig: LegacyHomeServerConnectionConfig) {
        // Here we migrate the DB, we copy the crypto DB to the location specific to Matrix SDK2, and we encrypt it.
        val userMd5 = legacyConfig.credentials.userId.md5()

        val sessionId = legacyConfig.credentials.let { (if (it.deviceId.isNullOrBlank()) it.userId else "${it.userId}|${it.deviceId}").md5() }
        val newLocation = File(context.filesDir, sessionId)

        val keyAlias = "crypto_module_$userMd5"

        // Ensure newLocation does not exist (can happen in case of partial migration)
        newLocation.deleteRecursively()
        newLocation.mkdirs()

        Timber.d("Migration: create legacy realm configuration")

        val realmConfiguration = RealmConfiguration.Builder()
                .directory(File(context.filesDir, userMd5))
                .name("crypto_store.realm")
                .modules(RealmCryptoStoreModule())
                .schemaVersion(RealmCryptoStoreMigration.CRYPTO_STORE_SCHEMA_VERSION)
                .migration(RealmCryptoStoreMigration)
                .build()

        Timber.d("Migration: copy DB to encrypted DB")
        Realm.getInstance(realmConfiguration).use {
            // Move the DB to the new location, handled by Matrix SDK2
            it.writeEncryptedCopyTo(File(newLocation, realmConfiguration.realmFileName), realmKeysUtils.getRealmEncryptionKey(keyAlias))
        }
    }

    // Delete all the files created by Riot Android which will not be used anymore by Element
    private fun clearFileSystem(legacyConfig: LegacyHomeServerConnectionConfig) {
        val cryptoFolder = legacyConfig.credentials.userId.md5()

        listOf(
                // Where session store was saved (we do not care about migrating that, an initial sync will be performed)
                File(context.filesDir, "MXFileStore"),
                // Previous (and very old) file crypto store
                File(context.filesDir, "MXFileCryptoStore"),
                // Draft. They will be lost, this is sad but we assume it
                File(context.filesDir, "MXLatestMessagesStore"),
                // Media storage
                File(context.filesDir, "MXMediaStore"),
                File(context.filesDir, "MXMediaStore2"),
                File(context.filesDir, "MXMediaStore3"),
                // Ext folder
                File(context.filesDir, "ext_share"),
                // Crypto store
                File(context.filesDir, cryptoFolder)
        ).forEach { file ->
            try {
                file.deleteRecursively()
            } catch (t: Throwable) {
                Timber.e(t, "Migration: unable to delete $file")
            }
        }
    }

    private fun clearSharedPrefs() {
        // Shared Pref. Note that we do not delete the default preferences, as it should be nearly the same (TODO check that)
        listOf(
                "Vector.LoginStorage",
                "GcmRegistrationManager",
                "IntegrationManager.Storage"
        ).forEach { prefName ->
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
        }
    }
}
