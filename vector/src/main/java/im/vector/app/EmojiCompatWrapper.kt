/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
