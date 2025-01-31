/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.aggregation.livelocation

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.util.md5
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.database.awaitTransaction
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import timber.log.Timber
import javax.inject.Inject

/**
 * Worker dedicated to update live location summary data so that it is considered as deactivated.
 * For the context: it is needed since a live location share should be deactivated after a certain timeout.
 */
internal class DeactivateLiveLocationShareWorker(context: Context, params: WorkerParameters, sessionManager: SessionManager) :
        SessionSafeCoroutineWorker<DeactivateLiveLocationShareWorker.Params>(
                context,
                params,
                sessionManager,
                Params::class.java
        ) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            override val lastFailureMessage: String? = null,
            val eventId: String,
            val roomId: String
    ) : SessionWorkerParams

    @SessionDatabase
    @Inject lateinit var realmConfiguration: RealmConfiguration

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        return runCatching {
            deactivateLiveLocationShare(params)
        }.fold(
                onSuccess = {
                    Result.success()
                },
                onFailure = {
                    Timber.e("failed to deactivate live, eventId: ${params.eventId}, roomId: ${params.roomId}")
                    Result.failure()
                }
        )
    }

    private suspend fun deactivateLiveLocationShare(params: Params) {
        awaitTransaction(realmConfiguration) { realm ->
            Timber.d("deactivating live with id=${params.eventId}")
            val aggregatedSummary = LiveLocationShareAggregatedSummaryEntity.get(
                    realm = realm,
                    roomId = params.roomId,
                    eventId = params.eventId
            )
            aggregatedSummary?.isActive = false
        }
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }

    companion object {
        fun getWorkName(eventId: String, roomId: String): String {
            val hash = "$eventId$roomId".md5()
            return "DeactivateLiveLocationWork-$hash"
        }
    }
}
