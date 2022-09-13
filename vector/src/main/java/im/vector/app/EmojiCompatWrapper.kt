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
package im.vector.app

import android.content.Context
import androidx.core.provider.FontRequest
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.FontRequestEmojiCompatConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

fun interface EmojiSpanify {
    fun spanify(sequence: CharSequence): CharSequence
}

@Singleton
class EmojiCompatWrapper @Inject constructor(private val context: Context) : EmojiSpanify {

    private var initialized = false

    fun init(fontRequest: FontRequest) {
        // Use emoji compat for the benefit of emoji spans
        val config = FontRequestEmojiCompatConfig(context, fontRequest)
                // we want to replace all emojis with selected font
                .setReplaceAll(true)
        // Debug options
//                .setEmojiSpanIndicatorEnabled(true)
//                .setEmojiSpanIndicatorColor(Color.GREEN)
        EmojiCompat.init(config)
                .registerInitCallback(object : EmojiCompat.InitCallback() {
                    override fun onInitialized() {
                        Timber.v("Emoji compat onInitialized success ")
                        initialized = true
                    }

                    override fun onFailed(throwable: Throwable?) {
                        Timber.e(throwable, "Failed to init EmojiCompat")
                    }
                })
    }

    override fun spanify(sequence: CharSequence): CharSequence {
        if (initialized) {
            try {
                return EmojiCompat.get().process(sequence) ?: sequence
            } catch (throwable: Throwable) {
                // Defensive coding against error (should not happend as it is initialized)
                Timber.e(throwable, "Failed to init EmojiCompat")
                return sequence
            }
        } else {
            return sequence
        }
    }
}
