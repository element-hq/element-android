/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync.handler

import android.content.Context
import androidx.work.WorkerParameters
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.crypto.crosssigning.UpdateTrustWorker
import org.matrix.android.sdk.internal.crypto.crosssigning.UpdateTrustWorkerDataRepository
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.user.UserEntityFactory
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.logLimit
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import timber.log.Timber
import javax.inject.Inject

/**
 * Note: We reuse the same type [UpdateTrustWorker.Params], since the input data are the same.
 */
internal class UpdateUserWorker(context: Context, params: WorkerParameters, sessionManager: SessionManager) :
        SessionSafeCoroutineWorker<UpdateTrustWorker.Params>(context, params, sessionManager, UpdateTrustWorker.Params::class.java) {

    @SessionDatabase
    @Inject lateinit var monarchy: Monarchy
    @Inject lateinit var updateTrustWorkerDataRepository: UpdateTrustWorkerDataRepository
    @Inject lateinit var getProfileInfoTask: GetProfileInfoTask

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: UpdateTrustWorker.Params): Result {
        val userList = params.filename
                ?.let { updateTrustWorkerDataRepository.getParam(it) }
                ?.userIds
                ?: params.updatedUserIds.orEmpty()

        // List should not be empty, but let's avoid go further in case of empty list
        if (userList.isNotEmpty()) {
            Timber.v("## UpdateUserWorker - updating users: ${userList.logLimit()}")
            fetchAndUpdateUsers(userList)
        }

        cleanup(params)
        return Result.success()
    }

    private suspend fun fetchAndUpdateUsers(userIdsToFetch: Collection<String>) {
        fetchUsers(userIdsToFetch)
                .takeIf { it.isNotEmpty() }
                ?.saveLocally()
    }

    private suspend fun fetchUsers(userIdsToFetch: Collection<String>) = userIdsToFetch.mapNotNull {
        tryOrNull {
            val profileJson = getProfileInfoTask.execute(GetProfileInfoTask.Params(it))
            User.fromJson(it, profileJson)
        }
    }

    private suspend fun List<User>.saveLocally() {
        val userEntities = map { user -> UserEntityFactory.create(user) }
        Timber.d("## saveLocally()")
        monarchy.awaitTransaction {
            Timber.d("## saveLocally() - in transaction")
            it.insertOrUpdate(userEntities)
        }
        Timber.d("## saveLocally() - END")
    }

    private fun cleanup(params: UpdateTrustWorker.Params) {
        params.filename
                ?.let { updateTrustWorkerDataRepository.delete(it) }
    }

    override fun buildErrorParams(params: UpdateTrustWorker.Params, message: String): UpdateTrustWorker.Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }
}
