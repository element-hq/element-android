/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotx.features.reactions.data

import android.content.res.Resources
import com.squareup.moshi.Moshi
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenScope
import javax.inject.Inject

@ScreenScope
class EmojiDataSource @Inject constructor(
        resources: Resources
) {
    val rawData = resources.openRawResource(R.raw.emoji_picker_datasource)
            .use { input ->
                Moshi.Builder()
                        .build()
                        .adapter(EmojiData::class.java)
                        .fromJson(input.bufferedReader().use { it.readText() })
            }
            ?: EmojiData(emptyList(), emptyMap(), emptyMap())

    fun filterWith(query: String): List<EmojiItem> {
        val words = query.split("\\s".toRegex())

        // First add emojis with name matching query, sorted by name
        return (rawData.emojis.values
                .filter { emojiItem ->
                    emojiItem.name.contains(query, true)
                }
                .sortedBy { it.name } +
                // Then emojis with keyword matching any of the word in the query, sorted by name
                rawData.emojis.values
                        .filter { emojiItem ->
                            words.fold(true, { prev, word ->
                                prev && emojiItem.keywords.any { keyword -> keyword.contains(word, true) }
                            })
                        }
                        .sortedBy { it.name })
                // and ensure they will not be present twice
                .distinct()
    }

    fun getQuickReactions(): List<EmojiItem> {
        return listOf(
                "+1", // 👍
                "-1", // 👎
                "grinning", // 😄
                "tada", // 🎉
                "confused", // 😕
                "heart", // ❤️
                "rocket", // 🚀
                "eyes" // 👀
        ).mapNotNull { rawData.emojis[it] }
    }
}
