package im.vector.app.core.ui.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.ceil
import kotlin.math.max

/**
 * TextView that reserves space at the bottom for overlaying it with a footer, e.g. in a FrameLayout or RelativeLayout
 */
class FooteredTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
): AppCompatTextView(context, attrs, defStyleAttr) {

    var footerHeight: Int = 0
    var footerWidth: Int = 0
    //var widthLimit: Float = 0f

    // Workaround to RTL languages with non-RTL content messages aligning left instead of start
    private var requiredHorizontalCanvasMove = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // First, let super measure the content for our normal TextView use
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Default case
        requiredHorizontalCanvasMove = 0f

        // Get max available width
        //val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        //val widthLimit = if (widthMode == MeasureSpec.AT_MOST) { widthSize.toFloat() } else { Float.MAX_VALUE }
        val widthLimit = widthSize.toFloat()
        /*
        // Sometimes, widthLimit is not the actual limit, so remember it... ?
        if (this.widthLimit > widthLimit) {
            widthLimit = this.widthLimit
        } else {
            this.widthLimit = widthLimit
        }
         */

        val lastLine = layout.lineCount - 1

        // Let's check if the last line's text has the same RTL behaviour as the layout direction.
        val viewIsRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val lastVisibleCharacter = layout.getLineVisibleEnd(lastLine) - 1
        val looksLikeRtl = layout.isRtlCharAt(lastVisibleCharacter)
        if (looksLikeRtl != viewIsRtl) {
            // Our footer would overlap text even if there is space in the last line, so reserve space in y-direction
            setMeasuredDimension(max(measuredWidth, footerWidth), measuredHeight + footerHeight)
            return
        }

        // Get required width for all lines
        var maxLineWidth = 0f
        for (i in 0 until layout.lineCount) {
            maxLineWidth = max(layout.getLineWidth(i), maxLineWidth)
        }

        // Fix wrap_content in multi-line texts by using maxLineWidth instead of measuredWidth here
        // (compare WrapWidthTextView.kt)
        var newWidth = ceil(maxLineWidth).toInt()
        var newHeight = measuredHeight

        val widthLastLine = layout.getLineWidth(lastLine)

        // Required width if putting footer in the same line as the last line
        val widthWithHorizontalFooter = widthLastLine + footerWidth

        // Is there space for a horizontal footer?
        if (widthWithHorizontalFooter <= widthLimit) {
            // Reserve extra horizontal footer space if necessary
            if (widthWithHorizontalFooter > newWidth) {
                newWidth = ceil(widthWithHorizontalFooter).toInt()

                if (viewIsRtl) {
                    requiredHorizontalCanvasMove = widthWithHorizontalFooter - measuredWidth
                }
            }
        } else {
            // Reserve vertical footer space
            newHeight += footerHeight
        }

        setMeasuredDimension(newWidth, newHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        // Workaround to RTL languages with non-RTL content messages aligning left instead of start
        if (requiredHorizontalCanvasMove > 0f) {
            canvas?.translate(requiredHorizontalCanvasMove, 0f)
        }

        super.onDraw(canvas)
    }
}
