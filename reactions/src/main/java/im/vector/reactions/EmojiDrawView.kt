package im.vector.reactions

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.text.Layout
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max


/**
 * We want to use a custom view for rendering an emoji.
 * With generic textview, the performance in the recycler view are very bad
 */
class EmojiDrawView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var mLayout: StaticLayout? = null

//    var _mySpacing = 0f

    var emoji: String? = null
        set(value) {
            field = value
            if (value != null) {
                EmojiRecyclerAdapter.beginTraceSession("EmojiDrawView.TextStaticLayout")
//                GlobalScope.launch {
//                    val sl = StaticLayout(value, tPaint, emojiSize, Layout.Alignment.ALIGN_CENTER, 1f, 0f, true)
//                    GlobalScope.launch(Dispatchers.Main) {
//                        if (emoji == value) {
//                            mLayout = sl
//                            //invalidate()
//                        }
//                    }
//                }
                mLayout = StaticLayout(value, tPaint, emojiSize, Layout.Alignment.ALIGN_CENTER, 1f, 0f, true)
                EmojiRecyclerAdapter.endTraceSession()
            } else {
                mLayout = null
            }
        }

    override fun onDraw(canvas: Canvas?) {
        EmojiRecyclerAdapter.beginTraceSession("EmojiDrawView.onDraw")
        super.onDraw(canvas)
        canvas?.save()
        val space = abs((width - emojiSize) / 2f)
        if (mLayout == null) {
            canvas?.drawCircle(width / 2f ,width / 2f, emojiSize / 2f,tPaint)
        } else {
            canvas?.translate(space, space)
            mLayout!!.draw(canvas)
        }
        canvas?.restore()
        EmojiRecyclerAdapter.endTraceSession()
    }

    companion object {
        private val tPaint = TextPaint()

        private var emojiSize = 40

        fun configureTextPaint(context: Context) {
            tPaint.isAntiAlias = true;
            tPaint.textSize = 24 * context.resources.displayMetrics.density
            tPaint.color = Color.LTGRAY
            emojiSize = tPaint.measureText("😅").toInt()
        }
    }

}