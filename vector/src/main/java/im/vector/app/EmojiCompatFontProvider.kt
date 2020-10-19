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
