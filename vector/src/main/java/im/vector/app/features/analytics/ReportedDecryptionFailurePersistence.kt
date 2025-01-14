/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics

import android.content.Context
import android.util.LruCache
import com.google.common.hash.BloomFilter
import com.google.common.hash.Funnels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

private const val REPORTED_UTD_FILE_NAME = "im.vector.analytics.reported_utd"
private const val EXPECTED_INSERTIONS = 5000

/**
 * This class is used to keep track of the reported decryption failures to avoid double reporting.
 * It uses a bloom filter to limit the memory/disk usage.
 */
class ReportedDecryptionFailurePersistence @Inject constructor(
        private val context: Context,
) {

    // Keep a cache of recent reported failures in memory.
    // They will be persisted to the a new bloom filter if the previous one is getting saturated.
    // Should be around 30KB max in memory.
    // Also allows to have 0% false positive rate for recent failures.
    private val inMemoryReportedFailures: LruCache<String, Unit> = LruCache(300)

    // Thread-safe and lock-free.
    // The expected insertions is 5000, and expected false positive probability of 3% when close to max capability.
    // The persisted size is expected to be around 5KB (100 times less than if it was raw strings).
    private var bloomFilter: BloomFilter<String> = BloomFilter.create<String>(Funnels.stringFunnel(Charsets.UTF_8), EXPECTED_INSERTIONS)

    /**
     * Mark an event as reported.
     * @param eventId the event id to mark as reported.
     */
    suspend fun markAsReported(eventId: String) {
        // Add to in memory cache.
        inMemoryReportedFailures.put(eventId, Unit)
        bloomFilter.put(eventId)

        // check if the filter is getting saturated? and then replace
        if (bloomFilter.approximateElementCount() > EXPECTED_INSERTIONS - 500) {
            // The filter is getting saturated, and the false positive rate is increasing.
            // It's time to replace the filter with a new one. And move the in-memory cache to the new filter.
            bloomFilter = BloomFilter.create<String>(Funnels.stringFunnel(Charsets.UTF_8), EXPECTED_INSERTIONS)
            inMemoryReportedFailures.snapshot().keys.forEach {
                bloomFilter.put(it)
            }
            persist()
        }
        Timber.v("## Bloom filter stats: expectedFpp: ${bloomFilter.expectedFpp()}, size: ${bloomFilter.approximateElementCount()}")
    }

    /**
     * Check if an event has been reported.
     * @param eventId the event id to check.
     * @return true if the event has been reported.
     */
    fun hasBeenReported(eventId: String): Boolean {
        // First check in memory cache.
        if (inMemoryReportedFailures.get(eventId) != null) {
            return true
        }
        return bloomFilter.mightContain(eventId)
    }

    /**
     * Load the reported failures from disk.
     */
    suspend fun load() {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.applicationContext.cacheDir, REPORTED_UTD_FILE_NAME)
                if (file.exists()) {
                    file.inputStream().use {
                        bloomFilter = BloomFilter.readFrom(it, Funnels.stringFunnel(Charsets.UTF_8))
                    }
                }
            } catch (e: Throwable) {
                Timber.e(e, "## Failed to load reported failures")
            }
        }
    }

    /**
     * Persist the reported failures to disk.
     */
    suspend fun persist() {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.applicationContext.cacheDir, REPORTED_UTD_FILE_NAME)
                if (!file.exists()) file.createNewFile()
                FileOutputStream(file).buffered().use {
                    bloomFilter.writeTo(it)
                }
                Timber.v("## Successfully saved reported failures, size: ${file.length()}")
            } catch (e: Throwable) {
                Timber.e(e, "## Failed to save reported failures")
            }
        }
    }
}
