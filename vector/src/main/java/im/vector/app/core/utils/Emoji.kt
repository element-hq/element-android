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

package im.vector.app.core.utils

import java.util.regex.Pattern

private val emojisPattern = Pattern.compile("((?:[\uD83C\uDF00-\uD83D\uDDFF]" +
        "|[\uD83E\uDD00-\uD83E\uDDFF]" +
        "|[\uD83D\uDE00-\uD83D\uDE4F]" +
        "|[\uD83D\uDE80-\uD83D\uDEFF]" +
        "|[\u2600-\u26FF]\uFE0F?" +
        "|[\u2700-\u27BF]\uFE0F?" +
        "|\u24C2\uFE0F?" +
        "|[\uD83C\uDDE6-\uD83C\uDDFF]{1,2}" +
        "|[\uD83C\uDD70\uD83C\uDD71\uD83C\uDD7E\uD83C\uDD7F\uD83C\uDD8E\uD83C\uDD91-\uD83C\uDD9A]\uFE0F?" +
        "|[\u0023\u002A\u0030-\u0039]\uFE0F?\u20E3" +
        "|[\u2194-\u2199\u21A9-\u21AA]\uFE0F?" +
        "|[\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55]\uFE0F?" +
        "|[\u2934\u2935]\uFE0F?" +
        "|[\u3030\u303D]\uFE0F?" +
        "|[\u3297\u3299]\uFE0F?" +
        "|[\uD83C\uDE01\uD83C\uDE02\uD83C\uDE1A\uD83C\uDE2F\uD83C\uDE32-\uD83C\uDE3A\uD83C\uDE50\uD83C\uDE51]\uFE0F?" +
        "|[\u203C\u2049]\uFE0F?" +
        "|[\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE]\uFE0F?" +
        "|[\u00A9\u00AE]\uFE0F?" +
        "|[\u2122\u2139]\uFE0F?" +
        "|\uD83C\uDC04\uFE0F?" +
        "|\uD83C\uDCCF\uFE0F?" +
        "|[\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA]\uFE0F?))")

/*
// A hashset from all supported emoji
private var knownEmojiSet: HashSet<String>? = null

fun initKnownEmojiHashSet(context: Context, done: (() -> Unit)? = null) {
    GlobalScope.launch {
        context.resources.openRawResource(R.raw.emoji_picker_datasource).use { input ->
            val moshi = Moshi.Builder().build()
            val jsonAdapter = moshi.adapter(EmojiData::class.java)
            val inputAsString = input.bufferedReader().use { it.readText() }
            val source = jsonAdapter.fromJson(inputAsString)
            knownEmojiSet = HashSet<String>().also {
                source?.emojis?.mapTo(it) { (_, value) ->
                    value.emojiString()
                }
            }
            done?.invoke()
        }
    }
}

fun isSingleEmoji(string: String): Boolean {
    if (knownEmojiSet == null) {
        Timber.e("Known Emoji Hashset not initialized")
        // use fallback regexp
        return containsOnlyEmojis(string)
    }
    return knownEmojiSet?.contains(string) ?: false
}
 */

/**
 * Test if a string contains emojis.
 * It seems that the regex [emoji_regex]+ does not work.
 * Some characters like ?, # or digit are accepted.
 *
 * @param str the body to test
 * @return true if the body contains only emojis
 */
fun containsOnlyEmojis(str: String?): Boolean {
    var res = false

    if (str != null && str.isNotEmpty()) {
        val matcher = emojisPattern.matcher(str)

        var start = -1
        var end = -1

        while (matcher.find()) {
            val nextStart = matcher.start()

            // first emoji position
            if (start < 0) {
                if (nextStart > 0) {
                    return false
                }
            } else {
                // must not have a character between
                if (nextStart != end) {
                    return false
                }
            }
            start = nextStart
            end = matcher.end()
        }

        res = -1 != start && end == str.length
    }

    return res
}

/**
 * Same as split, but considering emojis
 */
fun CharSequence.splitEmoji(): List<CharSequence> {
    val result = mutableListOf<CharSequence>()

    var index = 0

    while (index < length) {
        val firstChar = get(index)

        if (firstChar.toInt() == 0x200e) {
            // Left to right mark. What should I do with it?
        } else if (firstChar.toInt() in 0xD800..0xDBFF && index + 1 < length) {
            // We have the start of a surrogate pair
            val secondChar = get(index + 1)

            if (secondChar.toInt() in 0xDC00..0xDFFF) {
                // We have an emoji
                result.add("$firstChar$secondChar")
                index++
            } else {
                // Not sure what we have here...
                result.add("$firstChar")
            }
        } else {
            // Regular char
            result.add("$firstChar")
        }

        index++
    }

    return result
}
