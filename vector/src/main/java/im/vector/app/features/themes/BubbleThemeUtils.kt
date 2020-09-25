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

    // Special case of BUBBLE_STYLE_BOTH, to allow non-bubble items align to the sender either way
    // (not meant for user setting, but internal use)
    const val BUBBLE_STYLE_BOTH_HIDDEN = "both_hidden"
    // As above, so for single bubbles side
    const val BUBBLE_STYLE_START_HIDDEN = "start_hidden"

    private var mBubbleStyle: String = ""

    fun getBubbleStyle(context: Context): String {
        if (mBubbleStyle == "") {
            mBubbleStyle = PreferenceManager.getDefaultSharedPreferences(context).getString(BUBBLE_STYLE_KEY, BUBBLE_STYLE_BOTH)!!
        }
        return mBubbleStyle
    }

    fun drawsActualBubbles(bubbleStyle: String): Boolean {
        return bubbleStyle == BUBBLE_STYLE_START || bubbleStyle == BUBBLE_STYLE_BOTH
    }

    fun drawsDualSide(bubbleStyle: String): Boolean {
        return bubbleStyle == BUBBLE_STYLE_BOTH || bubbleStyle == BUBBLE_STYLE_BOTH_HIDDEN
    }

    fun invalidateBubbleStyle() {
        mBubbleStyle = ""
    }
}
