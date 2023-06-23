package me.gujun.android.span.style

import android.text.TextPaint
import android.text.style.CharacterStyle

class TextDecorationLineSpan(private val textDecorationLine: String) : CharacterStyle() {

  override fun updateDrawState(tp: TextPaint) {
    when (textDecorationLine) {
      "none" -> {
        tp.isUnderlineText = false
        tp.isStrikeThruText = false
      }
      "underline" -> {
        tp.isUnderlineText = true
        tp.isStrikeThruText = false
      }
      "line-through" -> {
        tp.isUnderlineText = false
        tp.isStrikeThruText = true
      }
      "underline line-through" -> {
        tp.isUnderlineText = true
        tp.isStrikeThruText = true
      }
      else -> throw RuntimeException("Unknown text decoration line")
    }
  }
}
