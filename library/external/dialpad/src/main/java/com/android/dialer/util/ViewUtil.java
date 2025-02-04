/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2012 The Android Open Source Project
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package com.android.dialer.util;

import android.graphics.Paint;
import android.util.TypedValue;
import android.widget.TextView;

/** Provides static functions to work with views */
public class ViewUtil {

  private ViewUtil() {}

  public static void resizeText(TextView textView, int originalTextSize, int minTextSize) {
    final Paint paint = textView.getPaint();
    final int width = textView.getWidth();
    if (width == 0) {
      return;
    }
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize);
    float ratio = width / paint.measureText(textView.getText().toString());
    if (ratio <= 1.0f) {
      textView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX, Math.max(minTextSize, originalTextSize * ratio));
    }
  }
}
