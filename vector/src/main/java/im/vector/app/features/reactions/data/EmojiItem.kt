/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.reactions.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Example:
 *  name: 'a',
 *  unified: 'b',
 *  non_qualified: 'c',
 *  has_img_apple: 'd',
 *  has_img_google: 'e',
 *  has_img_twitter: 'f',
 *  has_img_emojione: 'g',
 *  has_img_facebook: 'h',
 *  has_img_messenger: 'i',
 *  keywords: 'j',
 *  sheet: 'k',
 *  emoticons: 'l',
 *  text: 'm',
 *  short_names: 'n',
 *  added_in: 'o'.
 */
@JsonClass(generateAdapter = true)
data class EmojiItem(
        @Json(name = "a") val name: String,
        @Json(name = "b") val unicode: String,
        @Json(name = "j") val keywords: List<String> = emptyList()
) {
    // Cannot be private...
    var cache: String? = null

    val emoji: String
        get() {
            cache?.let { return it }

            // "\u0048\u0065\u006C\u006C\u006F World"
            val utf8Text = unicode
                    .split("-")
                    .joinToString("") { "\\u$it" }
            return fromUnicode(utf8Text)
                    .also { cache = it }
        }

    companion object {
        private fun fromUnicode(unicode: String): String {
            val arr = unicode
                    .replace("\\", "")
                    .split("u".toRegex())
                    .dropLastWhile { it.isEmpty() }
            return buildString {
                for (i in 1 until arr.size) {
                    val hexVal = Integer.parseInt(arr[i], 16)
                    append(Character.toChars(hexVal))
                }
            }
        }
    }
}
