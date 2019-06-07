package im.vector.riotredesign

import android.graphics.Typeface
import androidx.core.provider.FontsContractCompat
import timber.log.Timber


class EmojiCompatFontProvider : FontsContractCompat.FontRequestCallback() {

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
        if (!listeners.contains(listener)) {
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