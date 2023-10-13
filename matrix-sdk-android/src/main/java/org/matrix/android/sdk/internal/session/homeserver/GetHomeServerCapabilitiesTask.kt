/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.homeserver

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.auth.version.doesServerSupportLogoutDevices
import org.matrix.android.sdk.internal.auth.version.doesServerSupportQrCodeLogin
import org.matrix.android.sdk.internal.auth.version.doesServerSupportRedactionOfRelatedEvents
import org.matrix.android.sdk.internal.auth.version.doesServerSupportRemoteToggleOfPushNotifications
import org.matrix.android.sdk.internal.auth.version.doesServerSupportThreadUnreadNotifications
import org.matrix.android.sdk.internal.auth.version.doesServerSupportThreads
import org.matrix.android.sdk.internal.auth.version.isLoginAndRegistrationSupportedBySdk
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.integrationmanager.IntegrationManagerConfigExtractor
import org.matrix.android.sdk.internal.session.media.GetMediaConfigResult
import org.matrix.android.sdk.internal.session.media.MediaAPI
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.wellknown.GetWellknownTask
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

internal interface GetHomeServerCapabilitiesTask : Task<GetHomeServerCapabilitiesTask.Params, Unit> {
    data class Params(
            val forceRefresh: Boolean
    )
}

internal class DefaultGetHomeServerCapabilitiesTask @Inject constructor(
        private val capabilitiesAPI: CapabilitiesAPI,
        private val mediaAPI: MediaAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val globalErrorReceiver: GlobalErrorReceiver,
        private val getWellknownTask: GetWellknownTask,
        private val configExtractor: IntegrationManagerConfigExtractor,
        private val homeServerConnectionConfig: HomeServerConnectionConfig,
        @UserId
        private val userId: String
) : GetHomeServerCapabilitiesTask {

    override suspend fun execute(params: GetHomeServerCapabilitiesTask.Params) {
        var doRequest = params.forceRefresh
        if (!doRequest) {
            monarchy.awaitTransaction { realm ->
                val homeServerCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

                doRequest = homeServerCapabilitiesEntity.lastUpdatedTimestamp + MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS < Date().time
            }
        }

        if (!doRequest) {
            return
        }

        val capabilities = runCatching {
            executeRequest(globalErrorReceiver) {
                capabilitiesAPI.getCapabilities()
            }
        }.getOrNull()

        val mediaConfig = runCatching {
            executeRequest(globalErrorReceiver) {
                mediaAPI.getMediaConfig()
            }
        }.getOrNull()

        val versions = runCatching {
            executeRequest(null) {
                capabilitiesAPI.getVersions()
            }
        }.getOrNull()

        // Domain may include a port (eg, matrix.org:8080)
        // Per https://spec.matrix.org/latest/client-server-api/#well-known-uri we should extract the hostname from the server name
        // So we take everything before the last : as the domain for the well-known task.
        // NB: This is not always the same endpoint as capabilities / mediaConfig uses.
        val wellknownResult = runCatching {
            getWellknownTask.execute(
                    GetWellknownTask.Params(
                            domain = userId.getServerName().substringBeforeLast(":"),
                            homeServerConnectionConfig = homeServerConnectionConfig
                    )
            )
        }.getOrNull()

        insertInDb(capabilities, mediaConfig, versions, wellknownResult)
    }

    private suspend fun insertInDb(
            getCapabilitiesResult: GetCapabilitiesResult?,
            getMediaConfigResult: GetMediaConfigResult?,
            getVersionResult: Versions?,
            getWellknownResult: WellknownResult?
    ) {
        monarchy.awaitTransaction { realm ->
            val homeServerCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

            if (getCapabilitiesResult != null) {
                val capabilities = getCapabilitiesResult.capabilities

                // The spec says: If not present, the client should assume that
                // password, display name, avatar changes and 3pid changes are possible via the API
                homeServerCapabilitiesEntity.canChangePassword = capabilities?.changePassword?.enabled.orTrue()
                homeServerCapabilitiesEntity.canChangeDisplayName = capabilities?.changeDisplayName?.enabled.orTrue()
                homeServerCapabilitiesEntity.canChangeAvatar = capabilities?.changeAvatar?.enabled.orTrue()
                homeServerCapabilitiesEntity.canChange3pid = capabilities?.change3pid?.enabled.orTrue()

                homeServerCapabilitiesEntity.roomVersionsJson = capabilities?.roomVersions?.let {
                    MoshiProvider.providesMoshi().adapter(RoomVersions::class.java).toJson(it)
                }
            }

            if (getMediaConfigResult != null) {
                homeServerCapabilitiesEntity.maxUploadFileSize = getMediaConfigResult.maxUploadSize
                        ?: HomeServerCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN
            }

            if (getVersionResult != null) {
                homeServerCapabilitiesEntity.lastVersionIdentityServerSupported =
                        getVersionResult.isLoginAndRegistrationSupportedBySdk()
                homeServerCapabilitiesEntity.canControlLogoutDevices =
                        getVersionResult.doesServerSupportLogoutDevices()
                homeServerCapabilitiesEntity.canUseThreading = /* capabilities?.threads?.enabled.orFalse() || */
                        getVersionResult.doesServerSupportThreads()
                homeServerCapabilitiesEntity.canUseThreadReadReceiptsAndNotifications =
                        getVersionResult.doesServerSupportThreadUnreadNotifications()
                homeServerCapabilitiesEntity.canRemotelyTogglePushNotificationsOfDevices =
                        getVersionResult.doesServerSupportRemoteToggleOfPushNotifications()
                homeServerCapabilitiesEntity.canRedactEventWithRelations =
                        getVersionResult.doesServerSupportRedactionOfRelatedEvents()
            }

            if (getWellknownResult != null && getWellknownResult is WellknownResult.Prompt) {
                homeServerCapabilitiesEntity.defaultIdentityServerUrl = getWellknownResult.identityServerUrl
                // We are also checking for integration manager configurations
                val config = configExtractor.extract(getWellknownResult.wellKnown)
                if (config != null) {
                    Timber.v("Extracted integration config : $config")
                    realm.insertOrUpdate(config)
                }
                homeServerCapabilitiesEntity.authenticationIssuer = getWellknownResult.wellKnown.unstableDelegatedAuthConfig?.issuer
                homeServerCapabilitiesEntity.externalAccountManagementUrl = getWellknownResult.wellKnown.unstableDelegatedAuthConfig?.accountManagementUrl
                homeServerCapabilitiesEntity.disableNetworkConstraint = getWellknownResult.wellKnown.disableNetworkConstraint
            }

            homeServerCapabilitiesEntity.canLoginWithQrCode = canLoginWithQrCode(getCapabilitiesResult, getVersionResult)

            homeServerCapabilitiesEntity.lastUpdatedTimestamp = Date().time
        }
    }

    private fun canLoginWithQrCode(getCapabilitiesResult: GetCapabilitiesResult?, getVersionResult: Versions?): Boolean {
        // in r0 of MSC3882 an unstable feature was exposed. In stable it is done via /capabilities and /login

        // in stable 1.7 a capability is exposed for the authenticated user
        if (getCapabilitiesResult?.capabilities?.getLoginToken != null) {
            return getCapabilitiesResult.capabilities.getLoginToken.enabled == true
        }

        @Suppress("DEPRECATION")
        return getVersionResult?.doesServerSupportQrCodeLogin() == true
    }

    companion object {
        // 8 hours like on Element Web
        private const val MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS = 8 * 60 * 60 * 1000
    }
}
