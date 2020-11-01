package im.vector.app.features.themes

import android.content.Context
import android.graphics.Paint
import android.widget.TextView
import androidx.preference.PreferenceManager
import im.vector.app.features.home.room.detail.timeline.item.AnonymousReadReceipt

/**
 * Util class for managing themes.
 */
object BubbleThemeUtils {
    const val BUBBLE_STYLE_KEY = "BUBBLE_STYLE_KEY"
    const val BUBBLE_TIME_LOCATION_KEY = "BUBBLE_TIME_LOCATION_KEY"

    const val BUBBLE_STYLE_NONE = "none"
    const val BUBBLE_STYLE_START = "start"
    const val BUBBLE_STYLE_BOTH = "both"
    const val BUBBLE_TIME_TOP = "top"
    const val BUBBLE_TIME_BOTTOM = "bottom"

    // Special case of BUBBLE_STYLE_BOTH, to allow non-bubble items align to the sender either way
    // (not meant for user setting, but internal use)
    const val BUBBLE_STYLE_BOTH_HIDDEN = "both_hidden"
    // As above, so for single bubbles side
    const val BUBBLE_STYLE_START_HIDDEN = "start_hidden"

    private var mBubbleStyle: String = ""
    private var mBubbleTimeLocation: String = ""

    fun getBubbleStyle(context: Context): String {
        if (mBubbleStyle == "") {
            mBubbleStyle = PreferenceManager.getDefaultSharedPreferences(context).getString(BUBBLE_STYLE_KEY, BUBBLE_STYLE_BOTH)!!
        }
        return mBubbleStyle
    }

    fun getBubbleTimeLocation(context: Context): String {
        if (mBubbleTimeLocation == "") {
            mBubbleTimeLocation = PreferenceManager.getDefaultSharedPreferences(context).getString(BUBBLE_TIME_LOCATION_KEY, BUBBLE_TIME_BOTTOM)!!
        }
        if (!isBubbleTimeLocationSettingAllowed(context)) {
            return BUBBLE_TIME_TOP;
        }
        return mBubbleTimeLocation
    }

    fun getVisibleAnonymousReadReceipts(context: Context, readReceipt: AnonymousReadReceipt, sentByMe: Boolean): AnonymousReadReceipt {
        // TODO
        if (false) android.util.Log.e("SCSCSC", " " + context)
        return if (sentByMe && (/*TODO setting*/ true || readReceipt == AnonymousReadReceipt.PROCESSING)) {
            readReceipt
        } else {
            AnonymousReadReceipt.NONE
        }
    }

    fun drawsActualBubbles(bubbleStyle: String): Boolean {
        return bubbleStyle == BUBBLE_STYLE_START || bubbleStyle == BUBBLE_STYLE_BOTH
    }

    fun drawsDualSide(bubbleStyle: String): Boolean {
        return bubbleStyle == BUBBLE_STYLE_BOTH || bubbleStyle == BUBBLE_STYLE_BOTH_HIDDEN
    }

    fun invalidateBubbleStyle() {
        mBubbleStyle = ""
        mBubbleTimeLocation = ""
    }

    fun guessTextWidth(view: TextView): Float {
        return guessTextWidth(view, view.text)
    }

    fun guessTextWidth(view: TextView, text: CharSequence): Float {
        return guessTextWidth(view.textSize, text);
    }

    fun guessTextWidth(textSize: Float, text: CharSequence): Float {
        val paint = Paint()
        paint.textSize = textSize
        return paint.measureText(text.toString())
    }

    fun forceAlwaysShowTimestamps(bubbleStyle: String, bubbleTimeLocation: String): Boolean {
        return isBubbleTimeLocationSettingAllowed(bubbleStyle) && bubbleTimeLocation == BUBBLE_TIME_BOTTOM
    }

    fun isBubbleTimeLocationSettingAllowed(bubbleStyle: String): Boolean {
        return bubbleStyle == BUBBLE_STYLE_BOTH || bubbleStyle == BUBBLE_STYLE_BOTH_HIDDEN
    }

    fun forceAlwaysShowTimestamps(context: Context): Boolean {
        return forceAlwaysShowTimestamps(getBubbleStyle(context), getBubbleTimeLocation(context))
    }

    fun isBubbleTimeLocationSettingAllowed(context: Context): Boolean {
        return isBubbleTimeLocationSettingAllowed(getBubbleStyle(context))
    }
}
