/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2014 The Android Open Source Project
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package com.android.dialer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

import com.android.dialer.dialpadview.R;
import com.android.dialer.util.ViewUtil;

/** EditText which resizes dynamically with respect to text length. */
public class ResizingTextEditText extends EditText {

  private final int mOriginalTextSize;
  private final int mMinTextSize;

  public ResizingTextEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    mOriginalTextSize = (int) getTextSize();
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ResizingText);
    mMinTextSize =
        (int) a.getDimension(R.styleable.ResizingText_resizing_text_min_size, mOriginalTextSize);
    a.recycle();
  }

  @Override
  protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter);
    ViewUtil.resizeText(this, mOriginalTextSize, mMinTextSize);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    ViewUtil.resizeText(this, mOriginalTextSize, mMinTextSize);
  }
}
