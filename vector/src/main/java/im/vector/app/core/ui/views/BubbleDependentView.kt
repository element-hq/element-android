package im.vector.app.core.ui.views

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.children
import im.vector.app.features.themes.BubbleThemeUtils

interface BubbleDependentView<H> {

    fun messageBubbleAllowed(context: Context): Boolean {
        return false
    }

    fun shouldReverseBubble(): Boolean {
        return false
    }

    fun pseudoBubbleAllowed(): Boolean {
        return false
    }

    fun setBubbleLayout(holder: H, bubbleStyle: String, bubbleStyleSetting: String, reverseBubble: Boolean)
}

// This function belongs to BubbleDependentView, but turned out to raise a NoSuchMethodError since recently
// when called from an onImageSizeUpdated listener
fun <H>updateMessageBubble(context: Context, view: BubbleDependentView<H>, holder: H) {
    val bubbleStyleSetting = BubbleThemeUtils.getBubbleStyle(context)
    val bubbleStyle = when {
        view.messageBubbleAllowed(context)                                                      -> {
            bubbleStyleSetting
        }
        bubbleStyleSetting == BubbleThemeUtils.BUBBLE_STYLE_BOTH && view.pseudoBubbleAllowed()  -> {
            BubbleThemeUtils.BUBBLE_STYLE_BOTH_HIDDEN
        }
        bubbleStyleSetting == BubbleThemeUtils.BUBBLE_STYLE_START && view.pseudoBubbleAllowed() -> {
            BubbleThemeUtils.BUBBLE_STYLE_START_HIDDEN
        }
        else                                                                               -> {
            BubbleThemeUtils.BUBBLE_STYLE_NONE
        }
    }
    val reverseBubble = view.shouldReverseBubble() && BubbleThemeUtils.drawsDualSide(bubbleStyle)

    view.setBubbleLayout(holder, bubbleStyle, bubbleStyleSetting, reverseBubble)
}

fun setFlatRtl(layout: ViewGroup, direction: Int, childDirection: Int, depth: Int = 1) {
    layout.layoutDirection = direction
    for (child in layout.children) {
        if (depth > 1 && child is ViewGroup) {
            setFlatRtl(child, direction, childDirection, depth-1)
        } else {
            child.layoutDirection = childDirection
        }
    }
}
