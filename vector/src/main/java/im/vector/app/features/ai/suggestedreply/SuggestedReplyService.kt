/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.ai.suggestedreply

import im.vector.app.features.translation.TranslateConfig
import im.vector.app.features.translation.TranslationService
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestedReplyService @Inject constructor(
        private val translationService: TranslationService,
        private val config: TranslateConfig
) {
    suspend fun getSuggestions(recentMessages: String): List<String> {
        if (!config.enabled || !config.suggestedRepliesEnabled) return emptyList()

        val result = translationService.complete(
                "You are a helpful assistant that suggests short reply messages. " +
                        "Given the recent conversation, suggest exactly 3 short, natural replies the user could send. " +
                        "Reply ONLY with a JSON array of 3 strings, e.g. [\"reply1\", \"reply2\", \"reply3\"]. " +
                        "Keep each reply under 50 characters. Match the language of the conversation.",
                recentMessages
        ) ?: return emptyList()

        return parseSuggestions(result)
    }

    private fun parseSuggestions(response: String): List<String> {
        // Try JSON array first
        try {
            val jsonArray = JSONArray(response.trim())
            val suggestions = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                suggestions.add(jsonArray.getString(i))
            }
            if (suggestions.isNotEmpty()) return suggestions.take(3)
        } catch (e: Exception) {
            Timber.d("Failed to parse suggestions as JSON, trying line-by-line")
        }

        // Fallback: line-by-line parsing
        val lines = response.trim().lines()
                .map { it.trim().removePrefix("-").removePrefix("*").removePrefix("1.").removePrefix("2.").removePrefix("3.").trim() }
                .filter { it.isNotBlank() && it.length < 100 }
                .map { it.removeSurrounding("\"").removeSurrounding("'") }
                .take(3)
        return lines
    }
}
