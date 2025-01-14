/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2014 The Android Open Source Project
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package com.android.dialer.animation;

import android.view.animation.Interpolator;

import com.android.dialer.compat.PathInterpolatorCompat;

public class AnimUtils {
  public static final Interpolator EASE_OUT_EASE_IN =
      PathInterpolatorCompat.create(0.4f, 0, 0.2f, 1);
}
