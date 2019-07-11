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
package im.vector.riotx.features.reactions

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import im.vector.riotx.R


class EmojiDataSource(val context: Context) {

    var rawData: EmojiData? = null

    init {
        context.resources.openRawResource(R.raw.emoji_picker_datasource).use { input ->
            val moshi = Moshi.Builder().build()
            val jsonAdapter = moshi.adapter(EmojiData::class.java)
            val inputAsString = input.bufferedReader().use { it.readText() }
            this.rawData = jsonAdapter.fromJson(inputAsString)
           // this.rawData = mb.fr(InputStreamReader(it), EmojiData::class.java)
        }
    }
    @JsonClass(generateAdapter = true)
    data class EmojiData(val categories: List<EmojiCategory>,
                         val emojis: Map<String, EmojiItem>,
                         val aliases: Map<String, String>)

    @JsonClass(generateAdapter = true)
    data class EmojiCategory(val id: String, val name: String, val emojis: List<String>)

    @JsonClass(generateAdapter = true)
    data class EmojiItem(
            @Json(name = "a") val name: String,
            @Json(name = "b") val unicode: String,
            @Json(name = "j") val keywords: List<String>?,
            val k: List<String>?) {

        var _emojiText: String? = null

        fun emojiString() : String {
            if (_emojiText == null) {
                val utf8Text = unicode.split("-").joinToString("") { "\\u${it}" }//"\u0048\u0065\u006C\u006C\u006F World"
               _emojiText = fromUnicode(utf8Text)
            }
            return _emojiText!!
        }
    }

    companion object {
        fun fromUnicode(unicode: String): String {
            val str = unicode.replace("\\", "")
            val arr = str.split("u".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val text = StringBuffer()
            for (i in 1 until arr.size) {
                val hexVal = Integer.parseInt(arr[i], 16)
                text.append(Character.toChars(hexVal))
            }
            return text.toString()
        }
    }


//    name: 'a',
//    unified: 'b',
//    non_qualified: 'c',
//    has_img_apple: 'd',
//    has_img_google: 'e',
//    has_img_twitter: 'f',
//    has_img_emojione: 'g',
//    has_img_facebook: 'h',
//    has_img_messenger: 'i',
//    keywords: 'j',
//    sheet: 'k',
//    emoticons: 'l',
//    text: 'm',
//    short_names: 'n',
//    added_in: 'o',
}