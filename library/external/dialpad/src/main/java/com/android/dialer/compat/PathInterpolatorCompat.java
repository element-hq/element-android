/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.compat;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

public class PathInterpolatorCompat {

  public static Interpolator create(
      float controlX1, float controlY1, float controlX2, float controlY2) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new PathInterpolator(controlX1, controlY1, controlX2, controlY2);
    }
    return new PathInterpolatorBase(controlX1, controlY1, controlX2, controlY2);
  }

  private static class PathInterpolatorBase implements Interpolator {

    /** Governs the accuracy of the approximation of the {@link Path}. */
    private static final float PRECISION = 0.002f;

    private final float[] mX;
    private final float[] mY;

    public PathInterpolatorBase(Path path) {
      final PathMeasure pathMeasure = new PathMeasure(path, false /* forceClosed */);

      final float pathLength = pathMeasure.getLength();
      final int numPoints = (int) (pathLength / PRECISION) + 1;

      mX = new float[numPoints];
      mY = new float[numPoints];

      final float[] position = new float[2];
      for (int i = 0; i < numPoints; ++i) {
        final float distance = (i * pathLength) / (numPoints - 1);
        pathMeasure.getPosTan(distance, position, null /* tangent */);

        mX[i] = position[0];
        mY[i] = position[1];
      }
    }

    public PathInterpolatorBase(float controlX, float controlY) {
      this(createQuad(controlX, controlY));
    }

    public PathInterpolatorBase(
        float controlX1, float controlY1, float controlX2, float controlY2) {
      this(createCubic(controlX1, controlY1, controlX2, controlY2));
    }

    private static Path createQuad(float controlX, float controlY) {
      final Path path = new Path();
      path.moveTo(0.0f, 0.0f);
      path.quadTo(controlX, controlY, 1.0f, 1.0f);
      return path;
    }

    private static Path createCubic(
        float controlX1, float controlY1, float controlX2, float controlY2) {
      final Path path = new Path();
      path.moveTo(0.0f, 0.0f);
      path.cubicTo(controlX1, controlY1, controlX2, controlY2, 1.0f, 1.0f);
      return path;
    }

    @Override
    public float getInterpolation(float t) {
      if (t <= 0.0f) {
        return 0.0f;
      } else if (t >= 1.0f) {
        return 1.0f;
      }

      // Do a binary search for the correct x to interpolate between.
      int startIndex = 0;
      int endIndex = mX.length - 1;
      while (endIndex - startIndex > 1) {
        int midIndex = (startIndex + endIndex) / 2;
        if (t < mX[midIndex]) {
          endIndex = midIndex;
        } else {
          startIndex = midIndex;
        }
      }

      final float xRange = mX[endIndex] - mX[startIndex];
      if (xRange == 0) {
        return mY[startIndex];
      }

      final float tInRange = t - mX[startIndex];
      final float fraction = tInRange / xRange;

      final float startY = mY[startIndex];
      final float endY = mY[endIndex];

      return startY + (fraction * (endY - startY));
    }
  }
}
