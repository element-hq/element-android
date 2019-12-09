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

import android.content.Context
import com.squareup.moshi.Moshi
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenScope
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@ScreenScope
class EmojiDataSource @Inject constructor(
        context: Context
) {

    var rawData: EmojiData? = null

    init {
        measureTimeMillis {
            context.resources.openRawResource(R.raw.emoji_picker_datasource).use { input ->
                val moshi = Moshi.Builder().build()
                val jsonAdapter = moshi.adapter(EmojiData::class.java)
                val inputAsString = input.bufferedReader().use { it.readText() }
                this.rawData = jsonAdapter.fromJson(inputAsString)
            }
        }.also {
            Timber.e("Emoji: $it millis")
        }
    }
}
