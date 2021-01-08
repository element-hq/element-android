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
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.wellknown.WellknownResult
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.auth.version.isLoginAndRegistrationSupportedBySdk
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
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

internal interface GetHomeServerCapabilitiesTask : Task<Unit, Unit>

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

    override suspend fun execute(params: Unit) {
        var doRequest = false
        monarchy.awaitTransaction { realm ->
            val homeServerCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

            doRequest = homeServerCapabilitiesEntity.lastUpdatedTimestamp + MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS < Date().time
        }

        if (!doRequest) {
            return
        }

        val capabilities = runCatching {
            executeRequest<GetCapabilitiesResult>(globalErrorReceiver) {
                apiCall = capabilitiesAPI.getCapabilities()
            }
        }.getOrNull()

        val mediaConfig = runCatching {
            executeRequest<GetMediaConfigResult>(globalErrorReceiver) {
                apiCall = mediaAPI.getMediaConfig()
            }
        }.getOrNull()

        val versions = runCatching {
            executeRequest<Versions>(null) {
                apiCall = capabilitiesAPI.getVersions()
            }
        }.getOrNull()

        val wellknownResult = runCatching {
            getWellknownTask.execute(GetWellknownTask.Params(userId, homeServerConnectionConfig))
        }.getOrNull()

        insertInDb(capabilities, mediaConfig, versions, wellknownResult)
    }

    private suspend fun insertInDb(getCapabilitiesResult: GetCapabilitiesResult?,
                                   getMediaConfigResult: GetMediaConfigResult?,
                                   getVersionResult: Versions?,
                                   getWellknownResult: WellknownResult?) {
        monarchy.awaitTransaction { realm ->
            val homeServerCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

            if (getCapabilitiesResult != null) {
                homeServerCapabilitiesEntity.canChangePassword = getCapabilitiesResult.canChangePassword()
            }

            if (getMediaConfigResult != null) {
                homeServerCapabilitiesEntity.maxUploadFileSize = getMediaConfigResult.maxUploadSize
                        ?: HomeServerCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN
            }

            if (getVersionResult != null) {
                homeServerCapabilitiesEntity.lastVersionIdentityServerSupported = getVersionResult.isLoginAndRegistrationSupportedBySdk()
            }

            if (getWellknownResult != null && getWellknownResult is WellknownResult.Prompt) {
                homeServerCapabilitiesEntity.defaultIdentityServerUrl = getWellknownResult.identityServerUrl
                // We are also checking for integration manager configurations
                val config = configExtractor.extract(getWellknownResult.wellKnown)
                if (config != null) {
                    Timber.v("Extracted integration config : $config")
                    realm.insertOrUpdate(config)
                }
            }
            homeServerCapabilitiesEntity.lastUpdatedTimestamp = Date().time
        }
    }

    companion object {
        // 8 hours like on Riot Web
        private const val MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS = 8 * 60 * 60 * 1000
    }
}
