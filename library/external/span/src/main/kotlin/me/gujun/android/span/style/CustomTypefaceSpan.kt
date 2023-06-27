package me.gujun.android.span.style

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class CustomTypefaceSpan(private val tf: Typeface) : MetricAffectingSpan() {

  override fun updateMeasureState(paint: TextPaint) {
    apply(paint, tf)
  }

  override fun updateDrawState(ds: TextPaint) {
    apply(ds, tf)
  }

  private fun apply(paint: Paint, tf: Typeface) {
    val oldStyle: Int

    val old = paint.typeface
    oldStyle = old?.style ?: 0

    val fake = oldStyle and tf.style.inv()

    if (fake and Typeface.BOLD != 0) {
      paint.isFakeBoldText = true
    }

    if (fake and Typeface.ITALIC != 0) {
      paint.textSkewX = -0.25f
    }

    paint.typeface = tf
  }
}
