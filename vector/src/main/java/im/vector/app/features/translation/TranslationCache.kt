/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Translation cache stores translations in plain text on disk.
//  For E2EE rooms, ideally we should skip disk caching or encrypt the cache file.
//  This requires knowing whether a room is encrypted at cache-put time, which adds complexity.
//  For now, be aware that translated message content is persisted unencrypted.
@Singleton
class TranslationCache @Inject constructor(
        private val context: Context
) {
    private val memoryCache = LinkedHashMap<String, String>(5000, 0.75f, true)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveDiskJob: Job? = null

    companion object {
        private const val MAX_SIZE = 5000
        private const val CACHE_FILE = "translate-cache.json"
        private const val SAVE_DEBOUNCE_MS = 2000L
    }

    private fun key(text: String, lang: String) = "$lang:$text"

    fun get(text: String, lang: String): String? {
        return synchronized(memoryCache) { memoryCache[key(text, lang)] }
    }

    fun put(text: String, lang: String, translated: String) {
        synchronized(memoryCache) {
            if (memoryCache.size >= MAX_SIZE) {
                val firstKey = memoryCache.keys.firstOrNull()
                if (firstKey != null) {
                    memoryCache.remove(firstKey)
                }
            }
            memoryCache[key(text, lang)] = translated
        }
        scheduleDiskSave()
    }

    fun clear() {
        synchronized(memoryCache) { memoryCache.clear() }
        scope.launch {
            try {
                getCacheFile().delete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete cache file")
            }
        }
    }

    fun size(): Int = synchronized(memoryCache) { memoryCache.size }

    /**
     * Cancel pending coroutines. Good practice even though singletons live for app lifetime.
     */
    fun destroy() {
        scope.cancel()
    }

    fun loadFromDisk() {
        scope.launch {
            try {
                val file = getCacheFile()
                if (!file.exists()) return@launch
                val json = file.readText()
                val array = JSONArray(json)
                synchronized(memoryCache) {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        memoryCache[obj.getString("k")] = obj.getString("v")
                    }
                }
                Timber.d("Loaded ${array.length()} translations from disk cache")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load translation cache from disk")
            }
        }
    }

    private fun scheduleDiskSave() {
        saveDiskJob?.cancel()
        saveDiskJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveToDisk()
        }
    }

    private fun saveToDisk() {
        try {
            val entries = synchronized(memoryCache) {
                memoryCache.map { (k, v) -> k to v }
            }
            val array = JSONArray()
            entries.forEach { (k, v) ->
                array.put(JSONObject().apply {
                    put("k", k)
                    put("v", v)
                })
            }
            // Atomic write: write to temp file first, then rename
            val tempFile = File(context.filesDir, "$CACHE_FILE.tmp")
            tempFile.writeText(array.toString())
            tempFile.renameTo(getCacheFile())
            Timber.d("Saved ${entries.size} translations to disk cache")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save translation cache to disk")
        }
    }

    private fun getCacheFile(): File = File(context.filesDir, CACHE_FILE)
}
