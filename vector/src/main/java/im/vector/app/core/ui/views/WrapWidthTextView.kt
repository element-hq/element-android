/*
 * https://stackoverflow.com/questions/7439748/why-is-wrap-content-in-multiple-line-textview-filling-parent
 */

package im.vector.app.core.ui.views;

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.ceil
import kotlin.math.max

class WrapWidthTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val layout = this.layout ?: return
        val width = ceil(getMaxLineWidth(layout)).toInt() + compoundPaddingLeft + compoundPaddingRight
        val height = measuredHeight
        setMeasuredDimension(width, height)
    }

    private fun getMaxLineWidth(layout: Layout): Float {
        var maxWidth = 0.0f
        val lines = layout.lineCount
        for (i in 0 until lines) {
            maxWidth = max(maxWidth, layout.getLineWidth(i))
        }
        return maxWidth
    }
}
