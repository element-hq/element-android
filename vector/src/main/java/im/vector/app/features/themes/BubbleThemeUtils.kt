package im.vector.app.features.themes

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Util class for managing themes.
 */
object BubbleThemeUtils {
    const val BUBBLE_STYLE_KEY = "BUBBLE_STYLE_KEY"

    const val BUBBLE_STYLE_NONE = "none"
    const val BUBBLE_STYLE_START = "start"
    const val BUBBLE_STYLE_BOTH = "both"
    private var mBubbleStyle: String = ""

    fun getBubbleStyle(context: Context): String {
        if (mBubbleStyle == "") {
            mBubbleStyle = PreferenceManager.getDefaultSharedPreferences(context).getString(BUBBLE_STYLE_KEY, BUBBLE_STYLE_BOTH)!!
        }
        return mBubbleStyle
    }

    fun invalidateBubbleStyle() {
        mBubbleStyle = ""
    }
}
