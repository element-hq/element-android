/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.pushers

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.pushers.PusherState
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.RequestExecutor
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface AddPusherTask : Task<AddPusherTask.Params, Unit> {
    data class Params(val pusher: JsonPusher)
}

internal class DefaultAddPusherTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val requestExecutor: RequestExecutor,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AddPusherTask {
    override suspend fun execute(params: AddPusherTask.Params) {
        val pusher = params.pusher
        try {
            setPusher(pusher)
        } catch (error: Throwable) {
            monarchy.awaitTransaction { realm ->
                PusherEntity.where(realm, pusher.pushKey).findFirst()?.let {
                    it.state = PusherState.FAILED_TO_REGISTER
                }
            }
            throw error
        }
    }

    private suspend fun setPusher(pusher: JsonPusher) {
        requestExecutor.executeRequest(globalErrorReceiver) {
            pushersAPI.setPusher(pusher)
        }
        monarchy.awaitTransaction { realm ->
            val echo = PusherEntity.where(realm, pusher.pushKey).findFirst()
            if (echo == null) {
                pusher.toEntity().also {
                    it.state = PusherState.REGISTERED
                    realm.insertOrUpdate(it)
                }
            } else {
                echo.appDisplayName = pusher.appDisplayName
                echo.appId = pusher.appId
                echo.kind = pusher.kind
                echo.lang = pusher.lang
                echo.profileTag = pusher.profileTag
                echo.data?.format = pusher.data?.format
                echo.data?.url = pusher.data?.url
                echo.state = PusherState.REGISTERED
            }
        }
    }
}
