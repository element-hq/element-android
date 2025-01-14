/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app

import android.graphics.Typeface
import androidx.core.provider.FontsContractCompat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmojiCompatFontProvider @Inject constructor() : FontsContractCompat.FontRequestCallback() {

    var typeface: Typeface? = null
        set(value) {
            if (value != field) {
                field = value
                listeners.forEach {
                    try {
                        it.compatibilityFontUpdate(value)
                    } catch (t: Throwable) {
                        Timber.e(t)
                    }
                }
            }
        }

    private val listeners = ArrayList<FontProviderListener>()

    override fun onTypefaceRetrieved(typeface: Typeface) {
        this.typeface = typeface
    }

    override fun onTypefaceRequestFailed(reason: Int) {
        Timber.e("Failed to load Emoji Compatible font, reason:$reason")
    }

    fun addListener(listener: FontProviderListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: FontProviderListener) {
        listeners.remove(listener)
    }

    interface FontProviderListener {
        fun compatibilityFontUpdate(typeface: Typeface?)
    }
}
