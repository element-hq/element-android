/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.homeserver

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.wellknown.WellknownResult
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilities
import im.vector.matrix.android.internal.auth.version.Versions
import im.vector.matrix.android.internal.auth.version.isLoginAndRegistrationSupportedBySdk
import im.vector.matrix.android.internal.database.model.HomeServerCapabilitiesEntity
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.integrationmanager.IntegrationManagerConfigExtractor
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import im.vector.matrix.android.internal.wellknown.GetWellknownTask
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

internal interface GetHomeServerCapabilitiesTask : Task<Unit, Unit>

internal class DefaultGetHomeServerCapabilitiesTask @Inject constructor(
        private val capabilitiesAPI: CapabilitiesAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val eventBus: EventBus,
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
            executeRequest<GetCapabilitiesResult>(eventBus) {
                apiCall = capabilitiesAPI.getCapabilities()
            }
        }.getOrNull()

        val uploadCapabilities = runCatching {
            executeRequest<GetUploadCapabilitiesResult>(eventBus) {
                apiCall = capabilitiesAPI.getUploadCapabilities()
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

        insertInDb(capabilities, uploadCapabilities, versions, wellknownResult)
    }

    private suspend fun insertInDb(getCapabilitiesResult: GetCapabilitiesResult?,
                                   getUploadCapabilitiesResult: GetUploadCapabilitiesResult?,
                                   getVersionResult: Versions?,
                                   getWellknownResult: WellknownResult?) {
        monarchy.awaitTransaction { realm ->
            val homeServerCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

            if (getCapabilitiesResult != null) {
                homeServerCapabilitiesEntity.canChangePassword = getCapabilitiesResult.canChangePassword()
            }

            if (getUploadCapabilitiesResult != null) {
                homeServerCapabilitiesEntity.maxUploadFileSize = getUploadCapabilitiesResult.maxUploadSize
                        ?: HomeServerCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN
            }

            if (getVersionResult != null) {
                homeServerCapabilitiesEntity.lastVersionIdentityServerSupported = getVersionResult.isLoginAndRegistrationSupportedBySdk()
            }

            if (getWellknownResult != null && getWellknownResult is WellknownResult.Prompt) {
                homeServerCapabilitiesEntity.defaultIdentityServerUrl = getWellknownResult.identityServerUrl
                homeServerCapabilitiesEntity.adminE2EByDefault = getWellknownResult.wellKnown.e2eAdminSetting?.e2eDefault ?: true
                // We are also checking for integration manager configurations
                val config = configExtractor.extract(getWellknownResult.wellKnown)
                if (config != null) {
                    Timber.v("Extracted integration config : $config")
                    realm.insertOrUpdate(config)
                }
            } else {
                homeServerCapabilitiesEntity.adminE2EByDefault = true
            }
            homeServerCapabilitiesEntity.lastUpdatedTimestamp = Date().time
        }
    }

    companion object {
        // 8 hours like on Riot Web
        private const val MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS = 8 * 60 * 60 * 1000
    }
}
