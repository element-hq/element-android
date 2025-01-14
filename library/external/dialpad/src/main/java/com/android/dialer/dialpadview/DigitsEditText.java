/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2011 The Android Open Source Project
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package com.android.dialer.dialpadview;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import com.android.dialer.widget.ResizingTextEditText;

/** EditText which suppresses IME show up. */
public class DigitsEditText extends ResizingTextEditText {
  private OnTextContextMenuClickListener mOnTextContextMenuClickListener;

  public DigitsEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    setShowSoftInputOnFocus(false);
  }

  @Override
  protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
    super.onFocusChanged(focused, direction, previouslyFocusedRect);
    final InputMethodManager imm =
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
    if (imm != null && imm.isActive(this)) {
      imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    final boolean ret = super.onTouchEvent(event);
    // Must be done after super.onTouchEvent()
    final InputMethodManager imm =
        ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
    if (imm != null && imm.isActive(this)) {
      imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
    }
    return ret;
  }

  @Override
  protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter);
    if (isCursorVisible()) {
      setSelection(getText().length());
    }
  }

  @Override
  public boolean onTextContextMenuItem(int id) {
    boolean value = super.onTextContextMenuItem(id);
    if (mOnTextContextMenuClickListener != null) {
      mOnTextContextMenuClickListener.onTextContextMenuClickListener(id);
    }
    return value;
  }

  public interface OnTextContextMenuClickListener {
    void onTextContextMenuClickListener(int id);
  }

  public void setOnTextContextMenuClickListener(OnTextContextMenuClickListener listener) {
    this.mOnTextContextMenuClickListener = listener;
  }
}
