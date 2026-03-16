/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ai.notificationsummary

import im.vector.app.features.translation.TimelineTranslationManager
import im.vector.app.features.translation.TranslateConfig
import im.vector.app.features.translation.TranslationService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSummaryService @Inject constructor(
        private val translationService: TranslationService,
        private val config: TranslateConfig,
        private val timelineTranslationManager: TimelineTranslationManager
) {
    /**
     * Generate a summary of missed notifications using the LLM.
     * For each notification, checks if a translation exists in cache and uses it if available.
     * The summary is generated in French.
     *
     * @param notificationTexts list of pairs (roomName, messageBody)
     * @return the generated summary, or null if generation failed or feature is disabled
     */
    suspend fun generateSummary(notificationTexts: List<Pair<String, String>>): String? {
        if (!config.enabled || !config.notificationSummaryEnabled) return null
        if (notificationTexts.isEmpty()) return null

        val formatted = notificationTexts.joinToString("\n") { (room, message) ->
            // Check if a translation exists in cache for this message body
            val translatedMessage = timelineTranslationManager.getCachedTranslation(message)
            val textToUse = translatedMessage ?: message
            "[$room] $textToUse"
        }

        Timber.d("TRANSLATION_DEBUG NotificationSummary: generating summary for ${notificationTexts.size} notifications")

        return try {
            translationService.complete(
                    "Résume les notifications manquées suivantes de manière concise en français. " +
                            "Groupe par salon. Mentionne les mentions directes, les décisions, et les questions en attente.",
                    formatted
            )
        } catch (e: Exception) {
            Timber.e(e, "Notification summary generation failed")
            null
        }
    }
}
