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

package org.matrix.android.sdk.internal.session.profile

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.UserThreePidEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import timber.log.Timber
import javax.inject.Inject

internal abstract class RefreshUserThreePidsTask : Task<Unit, Unit>

internal class DefaultRefreshUserThreePidsTask @Inject constructor(private val profileAPI: ProfileAPI,
                                                                   @SessionDatabase private val monarchy: Monarchy,
                                                                   private val globalErrorReceiver: GlobalErrorReceiver) : RefreshUserThreePidsTask() {

    override suspend fun execute(params: Unit) {
        val accountThreePidsResponse = executeRequest(globalErrorReceiver) {
            profileAPI.getThreePIDs()
        }

        Timber.d("Get ${accountThreePidsResponse.threePids?.size} threePids")
        // Store the list in DB
        monarchy.awaitTransaction { realm ->
            realm.where(UserThreePidEntity::class.java).findAll().deleteAllFromRealm()
            accountThreePidsResponse.threePids?.forEach {
                val entity = UserThreePidEntity(
                        it.medium?.takeIf { med -> med in ThirdPartyIdentifier.SUPPORTED_MEDIUM } ?: return@forEach,
                        it.address ?: return@forEach,
                        it.validatedAt.toLong(),
                        it.addedAt.toLong())
                realm.insertOrUpdate(entity)
            }
        }
    }
}

private fun Any?.toLong(): Long {
    return when (this) {
        null      -> 0L
        is Long   -> this
        is Double -> this.toLong()
        else      -> 0L
    }
}
