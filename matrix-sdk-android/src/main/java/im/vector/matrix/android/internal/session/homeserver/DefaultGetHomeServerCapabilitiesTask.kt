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
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilities
import im.vector.matrix.android.internal.database.model.HomeServerCapabilitiesEntity
import im.vector.matrix.android.internal.database.query.getOrCreate
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import org.greenrobot.eventbus.EventBus
import java.util.*
import javax.inject.Inject

internal interface GetHomeServerCapabilitiesTask : Task<Unit, Unit>

internal class DefaultGetHomeServerCapabilitiesTask @Inject constructor(
        private val capabilitiesAPI: CapabilitiesAPI,
        private val monarchy: Monarchy,
        private val eventBus: EventBus
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

        val uploadCapabilities = executeRequest<GetUploadCapabilitiesResult>(eventBus) {
            apiCall = capabilitiesAPI.getUploadCapabilities()
        }

        // TODO Add other call here (get version, etc.)

        insertInDb(uploadCapabilities)
    }

    private suspend fun insertInDb(getUploadCapabilitiesResult: GetUploadCapabilitiesResult) {
        monarchy.awaitTransaction { realm ->
            val homeServerCapabilitiesEntity = HomeServerCapabilitiesEntity.getOrCreate(realm)

            homeServerCapabilitiesEntity.maxUploadFileSize = getUploadCapabilitiesResult.maxUploadSize
                    ?: HomeServerCapabilities.MAX_UPLOAD_FILE_SIZE_UNKNOWN

            homeServerCapabilitiesEntity.lastUpdatedTimestamp = Date().time
        }
    }

    companion object {
        // 8 hours like on Riot Web
        private const val MIN_DELAY_BETWEEN_TWO_REQUEST_MILLIS = 8 * 60 * 60 * 1000
    }
}
