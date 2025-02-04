/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2014 The Android Open Source Project
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package com.android.dialer.dialpadview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * This is a custom text view intended only for rendering the numerals (and star and pound) on the
 * dialpad. TextView has built in top/bottom padding to help account for ascenders/descenders.
 *
 * <p>Since vertical space is at a premium on the dialpad, particularly if the font size is scaled
 * to a larger default, for the dialpad we use this class to more precisely render characters
 * according to the precise amount of space they need.
 */
public class DialpadTextView extends TextView {

  private Rect mTextBounds = new Rect();
  private String mTextStr;

  public DialpadTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /** Draw the text to fit within the height/width which have been specified during measurement. */
  @Override
  public void draw(Canvas canvas) {
    Paint paint = getPaint();

    // Without this, the draw does not respect the style's specified text color.
    paint.setColor(getCurrentTextColor());

    // The text bounds values are relative and can be negative,, so rather than specifying a
    // standard origin such as 0, 0, we need to use negative of the left/top bounds.
    // For example, the bounds may be: Left: 11, Right: 37, Top: -77, Bottom: 0
    canvas.drawText(mTextStr, -mTextBounds.left, -mTextBounds.top, paint);
  }

  /**
   * Calculate the pixel-accurate bounds of the text when rendered, and use that to specify the
   * height and width.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    mTextStr = getText().toString();
    getPaint().getTextBounds(mTextStr, 0, mTextStr.length(), mTextBounds);

    int width = resolveSize(mTextBounds.width(), widthMeasureSpec);
    int height = resolveSize(mTextBounds.height(), heightMeasureSpec);
    setMeasuredDimension(width, height);
  }
}
