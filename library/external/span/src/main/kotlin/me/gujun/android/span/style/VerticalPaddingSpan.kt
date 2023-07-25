package me.gujun.android.span.style

import android.graphics.Paint.FontMetricsInt
import android.text.Spanned
import android.text.style.LineHeightSpan

class VerticalPaddingSpan(private val paddingTop: Int,
    private val paddingBottom: Int) : LineHeightSpan {

  private var flag: Boolean = true

  override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, v: Int,
      fm: FontMetricsInt) {
    text as Spanned
    /*val spanStart =*/ text.getSpanStart(this)
    val spanEnd = text.getSpanEnd(this)

//    Log.d("DEBUG", "Text: start=$start end=$end v=$v") // end may include the \n character
//    Log.d("DEBUG", "${text.slice(start until end)}".replace("\n", "#"))
//    Log.d("DEBUG", "VerticalPadding: spanStart=$spanStart spanEnd=$spanEnd spanstartv=$spanstartv")
//    Log.d("DEBUG", "$fm")
//    Log.d("DEBUG", "-----------------------")

    if (spanstartv == v) {
      fm.top -= paddingTop
      fm.ascent -= paddingTop
      flag = true
    } else if (flag && text[start - 1] != '\n') {
      fm.top += paddingTop
      fm.ascent += paddingTop
      flag = false
    } else {
      flag = false
    }

    if (end == spanEnd || end - 1 == spanEnd) {
      fm.descent += paddingBottom
      fm.bottom += paddingBottom
    }
  }
}
