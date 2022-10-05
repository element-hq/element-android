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

package org.matrix.android.sdk.internal.session.pushers

import org.matrix.android.sdk.api.session.pushers.PusherState
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface RemovePusherTask : Task<RemovePusherTask.Params, Unit> {
    data class Params(
            val pushKey: String,
            val pushAppId: String
    )
}

internal class DefaultRemovePusherTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        @SessionDatabase private val realmInstance: RealmInstance,
        private val globalErrorReceiver: GlobalErrorReceiver
) : RemovePusherTask {

    override suspend fun execute(params: RemovePusherTask.Params) {
        realmInstance.write {
            val existingEntity = PusherEntity.where(this, params.pushKey).first().find()
            existingEntity?.state = PusherState.UNREGISTERING
        }

        val realm = realmInstance.getRealm()
        val existing = PusherEntity.where(realm, params.pushKey).first().find()?.asDomain()
                ?: throw Exception("No existing pusher")

        val deleteBody = JsonPusher(
                pushKey = params.pushKey,
                appId = params.pushAppId,
                // kind null deletes the pusher
                kind = null,
                appDisplayName = existing.appDisplayName ?: "",
                deviceDisplayName = existing.deviceDisplayName ?: "",
                profileTag = existing.profileTag ?: "",
                lang = existing.lang,
                data = JsonPusherData(existing.data.url, existing.data.format),
                append = false
        )
        executeRequest(globalErrorReceiver) {
            pushersAPI.setPusher(deleteBody)
        }
        realmInstance.write {
            val pusherEntity = PusherEntity.where(this, params.pushKey).first().find() ?: return@write
            delete(pusherEntity)
        }
    }
}
