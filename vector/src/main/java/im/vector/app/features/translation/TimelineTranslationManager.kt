/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import android.text.Html
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages async translations for timeline messages.
 * When auto-translate is on, messages are translated in the background
 * and a listener is notified to refresh the timeline.
 */
@Singleton
class TimelineTranslationManager @Inject constructor(
        private val translationService: TranslationService,
        private val translateConfig: TranslateConfig,
        private val translationCache: TranslationCache
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingTranslations = mutableSetOf<String>()
    private val pendingLock = Any()
    private var listener: TranslationListener? = null

    interface TranslationListener {
        fun onTranslationReady(eventId: String)
    }

    fun setListener(listener: TranslationListener?) {
        this.listener = listener
    }

    /**
     * Strip the reply fallback from a Matrix plain-text body.
     * Reply fallback lines start with "> " and are followed by a blank line.
     */
    private fun stripReplyFallback(body: String): String {
        val lines = body.lines()
        var i = 0
        while (i < lines.size && lines[i].startsWith("> ")) {
            i++
        }
        // Skip the blank line after the reply fallback
        if (i > 0 && i < lines.size && lines[i].isBlank()) {
            i++
        }
        return if (i > 0) lines.drop(i).joinToString("\n") else body
    }

    /**
     * Extract the quoted text from a reply fallback.
     * Reply fallback lines start with "> ". The first line is usually "> <@user:server> "
     * and subsequent lines contain the actual quoted text.
     * Returns the quoted text (without "> " prefix) or null if no reply fallback found.
     */
    private fun extractReplyQuote(body: String): String? {
        val lines = body.lines()
        if (lines.isEmpty() || !lines[0].startsWith("> ")) return null
        val quoteLines = mutableListOf<String>()
        for (line in lines) {
            if (!line.startsWith("> ")) break
            quoteLines.add(line.removePrefix("> "))
        }
        if (quoteLines.isEmpty()) return null
        // The first line often contains the user mention like "<@user:server> quoted text"
        // The quoted text may be on the SAME line after the mention, or on subsequent lines.
        val firstLine = quoteLines[0]
        val processedFirst = if (firstLine.startsWith("<@") || firstLine.startsWith("* <@")) {
            // Extract text after the mention on the same line: "<@user:server> quoted text" → "quoted text"
            val afterMention = firstLine.replace(Regex("^\\*?\\s*<@[^>]+>\\s*"), "")
            afterMention.ifBlank { null }
        } else {
            firstLine
        }
        val textLines = mutableListOf<String>()
        if (processedFirst != null) textLines.add(processedFirst)
        if (quoteLines.size > 1) textLines.addAll(quoteLines.drop(1))
        val quoteText = textLines.joinToString("\n").trim()
        return if (quoteText.isNotBlank()) quoteText else null
    }

    private fun replyQuoteCacheKey(eventId: String) = "reply_quote:$eventId"

    /**
     * Replace Matrix user IDs like @user:server.org with just "user".
     */
    private fun stripMatrixIds(text: String): String {
        return text.replace(Regex("@([^:]+):[^\\s]+")) { matchResult ->
            matchResult.groupValues[1]
        }
    }

    /**
     * Strip HTML tags and decode HTML entities from text.
     */
    private fun stripHtml(text: String): String {
        @Suppress("DEPRECATION")
        return Html.fromHtml(text).toString().trim()
    }

    /**
     * Clean text before sending to the LLM for translation.
     */
    fun cleanTextForTranslation(body: String): String {
        var cleaned = stripReplyFallback(body)
        cleaned = stripMatrixIds(cleaned)
        // If text still contains HTML tags, strip them
        if (cleaned.contains(Regex("<[^>]+>"))) {
            cleaned = stripHtml(cleaned)
        }
        // Decode any remaining HTML entities
        if (cleaned.contains("&") && cleaned.contains(";")) {
            @Suppress("DEPRECATION")
            cleaned = Html.fromHtml(cleaned).toString().trim()
        }
        return cleaned.trim()
    }

    /**
     * Returns the cached translation if available, otherwise triggers async translation.
     * @return translated text or null if not yet available
     */
    fun getTranslatedText(eventId: String, body: String): String? {
        if (!translateConfig.enabled || !translateConfig.autoTranslate) return null
        val targetLang = translateConfig.targetLanguage

        val cleanedBody = cleanTextForTranslation(body)
        if (cleanedBody.isBlank()) return null

        // Extract reply quote (needed for both cached and non-cached paths)
        val replyQuote = extractReplyQuote(body)
        val cleanedQuote = if (replyQuote != null) {
            var cq = stripMatrixIds(replyQuote)
            if (cq.contains(Regex("<[^>]+>"))) cq = stripHtml(cq)
            if (cq.contains("&") && cq.contains(";")) {
                @Suppress("DEPRECATION")
                cq = Html.fromHtml(cq).toString().trim()
            }
            cq.takeIf { it.isNotBlank() }
        } else null

        // Check cache using cleaned body
        val cached = translationCache.get(cleanedBody, targetLang)
        if (cached != null) {
            // Body is cached, but we still need to ensure the reply quote is translated
            if (cleanedQuote != null) {
                val quoteCacheKey = replyQuoteCacheKey(eventId)
                if (translationCache.get(quoteCacheKey, targetLang) == null) {
                    // Reply quote not yet translated — do it async
                    scope.launch {
                        try {
                            val translatedQuote = translationService.translate(cleanedQuote, targetLang)
                            if (translatedQuote != null) {
                                translationCache.put(quoteCacheKey, targetLang, translatedQuote)
                                Timber.w("TRANSLATION_DEBUG Reply quote CACHED (late) for event $eventId: $translatedQuote")
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    listener?.onTranslationReady(eventId)
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Reply quote translation failed for event $eventId")
                        }
                    }
                }
            }
            return cached
        }

        // Trigger async translation if not already pending
        synchronized(pendingLock) {
            if (pendingTranslations.contains(eventId)) return null
            pendingTranslations.add(eventId)
        }

        scope.launch {
            try {
                val translated = translationService.translate(cleanedBody, targetLang)

                // Also translate the reply quote if present and not already cached
                if (cleanedQuote != null) {
                    val quoteCacheKey = replyQuoteCacheKey(eventId)
                    if (translationCache.get(quoteCacheKey, targetLang) == null) {
                        try {
                            val translatedQuote = translationService.translate(cleanedQuote, targetLang)
                            if (translatedQuote != null) {
                                translationCache.put(quoteCacheKey, targetLang, translatedQuote)
                                Timber.w("TRANSLATION_DEBUG Reply quote CACHED for event $eventId: $translatedQuote")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Reply quote translation failed for event $eventId")
                        }
                    }
                }

                if (translated != null) {
                    Timber.d("Translation ready for event $eventId")
                    // Notify on main thread
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        listener?.onTranslationReady(eventId)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Async translation failed for event $eventId")
            } finally {
                synchronized(pendingLock) {
                    pendingTranslations.remove(eventId)
                }
            }
        }
        return null
    }

    /**
     * Check if a translation is available in cache (synchronous, no async trigger).
     */
    fun getCachedTranslation(body: String): String? {
        if (!translateConfig.enabled || !translateConfig.autoTranslate) return null
        val cleanedBody = cleanTextForTranslation(body)
        if (cleanedBody.isBlank()) return null
        return translationCache.get(cleanedBody, translateConfig.targetLanguage)
    }

    /**
     * Get the translated reply quote for a given event, if available in cache.
     * This is the quote text that was translated alongside the main body in getTranslatedText().
     */
    fun getTranslatedReplyQuote(eventId: String): String? {
        if (!translateConfig.enabled || !translateConfig.autoTranslate) return null
        return translationCache.get(replyQuoteCacheKey(eventId), translateConfig.targetLanguage)
    }
}
