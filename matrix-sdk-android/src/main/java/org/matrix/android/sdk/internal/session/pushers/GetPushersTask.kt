/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.pushers

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.pushers.PusherState
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.model.deleteOnCascade
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface GetPushersTask : Task<Unit, Unit>

internal class DefaultGetPushersTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetPushersTask {

    override suspend fun execute(params: Unit) {
        val response = executeRequest(globalErrorReceiver) {
            pushersAPI.getPushers()
        }
        monarchy.awaitTransaction { realm ->
            // clear existings?
            realm.where(PusherEntity::class.java)
                    .findAll()
                    .forEach { it.deleteOnCascade() }
            response.pushers?.forEach { jsonPusher ->
                jsonPusher.toEntity().also {
                    it.state = PusherState.REGISTERED
                    realm.insertOrUpdate(it)
                }
            }
        }
    }
}
