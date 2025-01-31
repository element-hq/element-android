/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import com.squareup.moshi.JsonClass
import okio.buffer
import okio.source
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.io.File

@JsonClass(generateAdapter = true)
internal data class InitialSyncStatus(
        val step: Int = STEP_INIT,
        val downloadedDate: Long = 0
) {
    companion object {
        const val STEP_INIT = 0
        const val STEP_DOWNLOADING = 1
        const val STEP_DOWNLOADED = 2
        const val STEP_PARSED = 3
        const val STEP_SUCCESS = 4
    }
}

internal interface InitialSyncStatusRepository {
    fun getStep(): Int

    fun setStep(step: Int)
}

/**
 * This class handle the current status of an initial sync and persist it on the disk, to be robust against crash.
 */
internal class FileInitialSyncStatusRepository(
        directory: File,
        private val clock: Clock,
) : InitialSyncStatusRepository {

    companion object {
        // After 2 hours, we consider that the downloaded file is outdated:
        // - if a problem occurs, it's for big accounts, and big accounts have lots of new events in 2 hours
        // - For small accounts, there should be no problem, so 2 hours delay will never be used.
        private const val INIT_SYNC_FILE_LIFETIME = 2 * 60 * 60 * 1_000L
    }

    private val file = File(directory, "status.json")
    private val jsonAdapter = MoshiProvider.providesMoshi().adapter(InitialSyncStatus::class.java)

    private var cache: InitialSyncStatus? = null

    override fun getStep(): Int {
        ensureCache()
        val state = cache?.step ?: InitialSyncStatus.STEP_INIT
        return if (state >= InitialSyncStatus.STEP_DOWNLOADED &&
                clock.epochMillis() > (cache?.downloadedDate ?: 0) + INIT_SYNC_FILE_LIFETIME) {
            Timber.d("INIT_SYNC downloaded file is outdated, download it again")
            // The downloaded file is outdated
            setStep(InitialSyncStatus.STEP_INIT)
            InitialSyncStatus.STEP_INIT
        } else {
            state
        }
    }

    override fun setStep(step: Int) {
        var newStatus = cache?.copy(step = step) ?: InitialSyncStatus(step = step)
        if (step == InitialSyncStatus.STEP_DOWNLOADED) {
            // Also store the downloaded date
            newStatus = newStatus.copy(
                    downloadedDate = clock.epochMillis()
            )
        }
        cache = newStatus
        writeFile()
    }

    private fun ensureCache() {
        if (cache == null) readFile()
    }

    /**
     * File -> Cache.
     */
    private fun readFile() {
        cache = file
                .takeIf { it.exists() }
                ?.let { jsonAdapter.fromJson(it.source().buffer()) }
    }

    /**
     * Cache -> File.
     */
    private fun writeFile() {
        file.delete()
        cache
                ?.let { jsonAdapter.toJson(it) }
                ?.let { file.writeText(it) }
    }
}
