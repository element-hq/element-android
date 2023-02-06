/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.dialer.dialpadview;

import android.content.Context;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/**
 * Custom class for dialpad buttons.
 *
 * <p>When touch exploration mode is enabled for accessibility, this class implements the
 * lift-to-type interaction model:
 *
 * <ul>
 * <li>Hovering over the button will cause it to gain accessibility focus
 * <li>Removing the hover pointer while inside the bounds of the button will perform a click action
 * <li>If long-click is supported, hovering over the button for a longer period of time will switch
 *     to the long-click action
 * <li>Moving the hover pointer outside of the bounds of the button will restore to the normal click
 *     action
 * </ul>
 */
public class DialpadKeyButton extends FrameLayout {

  /** Timeout before switching to long-click accessibility mode. */
  private static final int LONG_HOVER_TIMEOUT = ViewConfiguration.getLongPressTimeout() * 2;

  /** Accessibility manager instance used to check touch exploration state. */
  private AccessibilityManager mAccessibilityManager;

  /** Bounds used to filter HOVER_EXIT events. */
  private RectF mHoverBounds = new RectF();

  /** Whether this view is currently in the long-hover state. */
  private boolean mLongHovered;

  /** Alternate content description for long-hover state. */
  private CharSequence mLongHoverContentDesc;

  /** Backup of standard content description. Used for accessibility. */
  private CharSequence mBackupContentDesc;

  /** Backup of clickable property. Used for accessibility. */
  private boolean mWasClickable;

  /** Backup of long-clickable property. Used for accessibility. */
  private boolean mWasLongClickable;

  /** Runnable used to trigger long-click mode for accessibility. */
  private Runnable mLongHoverRunnable;

  private OnPressedListener mOnPressedListener;

  public DialpadKeyButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    initForAccessibility(context);
  }

  public DialpadKeyButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initForAccessibility(context);
  }

  public void setOnPressedListener(OnPressedListener onPressedListener) {
    mOnPressedListener = onPressedListener;
  }

  private void initForAccessibility(Context context) {
    mAccessibilityManager =
        (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
  }

  public void setLongHoverContentDescription(CharSequence contentDescription) {
    mLongHoverContentDesc = contentDescription;

    if (mLongHovered) {
      super.setContentDescription(mLongHoverContentDesc);
    }
  }

  @Override
  public void setContentDescription(CharSequence contentDescription) {
    if (mLongHovered) {
      mBackupContentDesc = contentDescription;
    } else {
      super.setContentDescription(contentDescription);
    }
  }

  @Override
  public void setPressed(boolean pressed) {
    super.setPressed(pressed);
    if (mOnPressedListener != null) {
      mOnPressedListener.onPressed(this, pressed);
    }
  }

  @Override
  public void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    mHoverBounds.left = getPaddingLeft();
    mHoverBounds.right = w - getPaddingRight();
    mHoverBounds.top = getPaddingTop();
    mHoverBounds.bottom = h - getPaddingBottom();
  }

  @Override
  public boolean performAccessibilityAction(int action, Bundle arguments) {
    if (action == AccessibilityNodeInfo.ACTION_CLICK) {
      simulateClickForAccessibility();
      return true;
    }

    return super.performAccessibilityAction(action, arguments);
  }

  @Override
  public boolean onHoverEvent(MotionEvent event) {
    // When touch exploration is turned on, lifting a finger while inside
    // the button's hover target bounds should perform a click action.
    if (mAccessibilityManager.isEnabled() && mAccessibilityManager.isTouchExplorationEnabled()) {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_HOVER_ENTER:
          // Lift-to-type temporarily disables double-tap activation.
          mWasClickable = isClickable();
          mWasLongClickable = isLongClickable();
          if (mWasLongClickable && mLongHoverContentDesc != null) {
            if (mLongHoverRunnable == null) {
              mLongHoverRunnable =
                  new Runnable() {
                    @Override
                    public void run() {
                      setLongHovered(true);
                      announceForAccessibility(mLongHoverContentDesc);
                    }
                  };
            }
            postDelayed(mLongHoverRunnable, LONG_HOVER_TIMEOUT);
          }

          setClickable(false);
          setLongClickable(false);
          break;
        case MotionEvent.ACTION_HOVER_EXIT:
          if (mHoverBounds.contains(event.getX(), event.getY())) {
            if (mLongHovered) {
              performLongClick();
            } else {
              simulateClickForAccessibility();
            }
          }

          cancelLongHover();
          setClickable(mWasClickable);
          setLongClickable(mWasLongClickable);
          break;
      }
    }

    return super.onHoverEvent(event);
  }

  /**
   * When accessibility is on, simulate press and release to preserve the semantic meaning of
   * performClick(). Required for Braille support.
   */
  private void simulateClickForAccessibility() {
    // Checking the press state prevents double activation.
    if (isPressed()) {
      return;
    }

    setPressed(true);

    // Stay consistent with performClick() by sending the event after
    // setting the pressed state but before performing the action.
    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);

    setPressed(false);
  }

  private void setLongHovered(boolean enabled) {
    if (mLongHovered != enabled) {
      mLongHovered = enabled;

      // Switch between normal and alternate description, if available.
      if (enabled) {
        mBackupContentDesc = getContentDescription();
        super.setContentDescription(mLongHoverContentDesc);
      } else {
        super.setContentDescription(mBackupContentDesc);
      }
    }
  }

  private void cancelLongHover() {
    if (mLongHoverRunnable != null) {
      removeCallbacks(mLongHoverRunnable);
    }
    setLongHovered(false);
  }

  public interface OnPressedListener {

    void onPressed(View view, boolean pressed);
  }
}
