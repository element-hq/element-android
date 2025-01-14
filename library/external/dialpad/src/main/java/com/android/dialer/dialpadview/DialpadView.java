/*
 * Copyright 2024 New Vector Ltd.
 * Copyright 2014 The Android Open Source Project
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package com.android.dialer.dialpadview;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.animation.AnimUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/** View that displays a twelve-key phone dialpad. */
public class DialpadView extends LinearLayout {

  private static final String TAG = DialpadView.class.getSimpleName();

  private static final double DELAY_MULTIPLIER = 0.66;
  private static final double DURATION_MULTIPLIER = 0.8;
  // For animation.
  private static final int KEY_FRAME_DURATION = 33;
  /** {@code True} if the dialpad is in landscape orientation. */
  private final boolean mIsLandscape;
  /** {@code True} if the dialpad is showing in a right-to-left locale. */
  private final boolean mIsRtl;

  private final int[] mButtonIds =
      new int[] {
        R.id.zero,
        R.id.one,
        R.id.two,
        R.id.three,
        R.id.four,
        R.id.five,
        R.id.six,
        R.id.seven,
        R.id.eight,
        R.id.nine,
        R.id.star,
        R.id.pound
      };
  private EditText mDigits;
  private ImageButton mDelete;
  private View mOverflowMenuButton;
  private ViewGroup mRateContainer;
  private TextView mIldCountry;
  private TextView mIldRate;
  private boolean mCanDigitsBeEdited;
  private int mTranslateDistance;

  public DialpadView(Context context) {
    this(context, null);
  }

  public DialpadView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DialpadView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mTranslateDistance =
        getResources().getDimensionPixelSize(R.dimen.dialpad_key_button_translate_y);

    mIsLandscape =
        getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    mIsRtl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
        TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    setupKeypad();
    mDigits = (EditText) findViewById(R.id.digits);
    mDelete = (ImageButton) findViewById(R.id.deleteButton);
    mOverflowMenuButton = findViewById(R.id.dialpad_overflow);
    mRateContainer = (ViewGroup) findViewById(R.id.rate_container);
    mIldCountry = (TextView) mRateContainer.findViewById(R.id.ild_country);
    mIldRate = (TextView) mRateContainer.findViewById(R.id.ild_rate);

    AccessibilityManager accessibilityManager =
        (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
    if (accessibilityManager.isEnabled()) {
      // The text view must be selected to send accessibility events.
      mDigits.setSelected(true);
    }
  }

  private void setupKeypad() {
    final int[] letterIds =
        new int[] {
          R.string.dialpad_0_letters,
          R.string.dialpad_1_letters,
          R.string.dialpad_2_letters,
          R.string.dialpad_3_letters,
          R.string.dialpad_4_letters,
          R.string.dialpad_5_letters,
          R.string.dialpad_6_letters,
          R.string.dialpad_7_letters,
          R.string.dialpad_8_letters,
          R.string.dialpad_9_letters,
          R.string.dialpad_star_letters,
          R.string.dialpad_pound_letters
        };

    final Resources resources = getContext().getResources();

    DialpadKeyButton dialpadKey;
    TextView numberView;
    TextView lettersView;

    final Locale currentLocale = resources.getConfiguration().locale;
    final NumberFormat nf;
    // We translate dialpad numbers only for "fa" and not any other locale
    // ("ar" anybody ?).
    if ("fa".equals(currentLocale.getLanguage())) {
      nf = DecimalFormat.getInstance(resources.getConfiguration().locale);
    } else {
      nf = DecimalFormat.getInstance(Locale.ENGLISH);
    }

    for (int i = 0; i < mButtonIds.length; i++) {
      dialpadKey = (DialpadKeyButton) findViewById(mButtonIds[i]);
      numberView = (TextView) dialpadKey.findViewById(R.id.dialpad_key_number);
      lettersView = (TextView) dialpadKey.findViewById(R.id.dialpad_key_letters);

      final String numberString;
      final CharSequence numberContentDescription;
      if (mButtonIds[i] == R.id.pound) {
        numberString = resources.getString(R.string.dialpad_pound_number);
        numberContentDescription = numberString;
      } else if (mButtonIds[i] == R.id.star) {
        numberString = resources.getString(R.string.dialpad_star_number);
        numberContentDescription = numberString;
      } else {
        numberString = nf.format(i);
        // The content description is used for Talkback key presses. The number is
        // separated by a "," to introduce a slight delay. Convert letters into a verbatim
        // span so that they are read as letters instead of as one word.
        String letters = resources.getString(letterIds[i]);
        Spannable spannable =
            Spannable.Factory.getInstance().newSpannable(numberString + "," + letters);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          spannable.setSpan(
              (new TtsSpan.VerbatimBuilder(letters)).build(),
              numberString.length() + 1,
              numberString.length() + 1 + letters.length(),
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        numberContentDescription = spannable;
      }

      numberView.setText(numberString);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        numberView.setElegantTextHeight(false);
      }
      dialpadKey.setContentDescription(numberContentDescription);

      if (lettersView != null) {
        lettersView.setText(resources.getString(letterIds[i]));
      }
    }

    final DialpadKeyButton one = (DialpadKeyButton) findViewById(R.id.one);
    one.setLongHoverContentDescription(resources.getText(R.string.description_voicemail_button));

    final DialpadKeyButton zero = (DialpadKeyButton) findViewById(R.id.zero);
    zero.setLongHoverContentDescription(resources.getText(R.string.description_image_button_plus));
  }

  public void setShowVoicemailButton(boolean show) {
    View view = findViewById(R.id.dialpad_key_voicemail);
    if (view != null) {
      view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }
  }

  /**
   * Whether or not the digits above the dialer can be edited.
   *
   * @param canBeEdited If true, the backspace button will be shown and the digits EditText will be
   *     configured to allow text manipulation.
   */
  public void setCanDigitsBeEdited(boolean canBeEdited) {
//    View deleteButton = findViewById(R.id.deleteButton);
//    deleteButton.setVisibility(canBeEdited ? View.VISIBLE : View.INVISIBLE);
//    View overflowMenuButton = findViewById(R.id.dialpad_overflow);
//    overflowMenuButton.setVisibility(canBeEdited ? View.VISIBLE : View.GONE);

//    EditText digits = (EditText) findViewById(R.id.digits);
//    digits.setClickable(canBeEdited);
//    digits.setLongClickable(canBeEdited);
//    digits.setFocusableInTouchMode(canBeEdited);
//    digits.setCursorVisible(false);

    mCanDigitsBeEdited = canBeEdited;
  }

  public void setCallRateInformation(String countryName, String displayRate) {
    if (TextUtils.isEmpty(countryName) && TextUtils.isEmpty(displayRate)) {
      mRateContainer.setVisibility(View.GONE);
      return;
    }
    mRateContainer.setVisibility(View.VISIBLE);
    mIldCountry.setText(countryName);
    mIldRate.setText(displayRate);
  }

  public boolean canDigitsBeEdited() {
    return mCanDigitsBeEdited;
  }

  /**
   * Always returns true for onHoverEvent callbacks, to fix problems with accessibility due to the
   * dialpad overlaying other fragments.
   */
  @Override
  public boolean onHoverEvent(MotionEvent event) {
    return true;
  }

  public void animateShow() {
    // This is a hack; without this, the setTranslationY is delayed in being applied, and the
    // numbers appear at their original position (0) momentarily before animating.
    final AnimatorListenerAdapter showListener = new AnimatorListenerAdapter() {};

    for (int i = 0; i < mButtonIds.length; i++) {
      int delay = (int) (getKeyButtonAnimationDelay(mButtonIds[i]) * DELAY_MULTIPLIER);
      int duration = (int) (getKeyButtonAnimationDuration(mButtonIds[i]) * DURATION_MULTIPLIER);
      final DialpadKeyButton dialpadKey = (DialpadKeyButton) findViewById(mButtonIds[i]);

      ViewPropertyAnimator animator = dialpadKey.animate();
      if (mIsLandscape) {
        // Landscape orientation requires translation along the X axis.
        // For RTL locales, ensure we translate negative on the X axis.
        dialpadKey.setTranslationX((mIsRtl ? -1 : 1) * mTranslateDistance);
        animator.translationX(0);
      } else {
        // Portrait orientation requires translation along the Y axis.
        dialpadKey.setTranslationY(mTranslateDistance);
        animator.translationY(0);
      }
      animator
          .setInterpolator(AnimUtils.EASE_OUT_EASE_IN)
          .setStartDelay(delay)
          .setDuration(duration)
          .setListener(showListener)
          .start();
    }
  }

  public EditText getDigits() {
    return mDigits;
  }

  public ImageButton getDeleteButton() {
    return mDelete;
  }

  public View getOverflowMenuButton() {
    return mOverflowMenuButton;
  }

  /**
   * Get the animation delay for the buttons, taking into account whether the dialpad is in
   * landscape left-to-right, landscape right-to-left, or portrait.
   *
   * @param buttonId The button ID.
   * @return The animation delay.
   */
  private int getKeyButtonAnimationDelay(int buttonId) {
    if (mIsLandscape) {
      if (mIsRtl) {
        if (buttonId == R.id.three) {
          return KEY_FRAME_DURATION * 1;
        } else if (buttonId == R.id.six) {
          return KEY_FRAME_DURATION * 2;
        } else if (buttonId == R.id.nine) {
          return KEY_FRAME_DURATION * 3;
        } else if (buttonId == R.id.pound) {
          return KEY_FRAME_DURATION * 4;
        } else if (buttonId == R.id.two) {
          return KEY_FRAME_DURATION * 5;
        } else if (buttonId == R.id.five) {
          return KEY_FRAME_DURATION * 6;
        } else if (buttonId == R.id.eight) {
          return KEY_FRAME_DURATION * 7;
        } else if (buttonId == R.id.zero) {
          return KEY_FRAME_DURATION * 8;
        } else if (buttonId == R.id.one) {
          return KEY_FRAME_DURATION * 9;
        } else if (buttonId == R.id.four) {
          return KEY_FRAME_DURATION * 10;
        } else if (buttonId == R.id.seven || buttonId == R.id.star) {
          return KEY_FRAME_DURATION * 11;
        }
      } else {
        if (buttonId == R.id.one) {
          return KEY_FRAME_DURATION * 1;
        } else if (buttonId == R.id.four) {
          return KEY_FRAME_DURATION * 2;
        } else if (buttonId == R.id.seven) {
          return KEY_FRAME_DURATION * 3;
        } else if (buttonId == R.id.star) {
          return KEY_FRAME_DURATION * 4;
        } else if (buttonId == R.id.two) {
          return KEY_FRAME_DURATION * 5;
        } else if (buttonId == R.id.five) {
          return KEY_FRAME_DURATION * 6;
        } else if (buttonId == R.id.eight) {
          return KEY_FRAME_DURATION * 7;
        } else if (buttonId == R.id.zero) {
          return KEY_FRAME_DURATION * 8;
        } else if (buttonId == R.id.three) {
          return KEY_FRAME_DURATION * 9;
        } else if (buttonId == R.id.six) {
          return KEY_FRAME_DURATION * 10;
        } else if (buttonId == R.id.nine || buttonId == R.id.pound) {
          return KEY_FRAME_DURATION * 11;
        }
      }
    } else {
      if (buttonId == R.id.one) {
        return KEY_FRAME_DURATION * 1;
      } else if (buttonId == R.id.two) {
        return KEY_FRAME_DURATION * 2;
      } else if (buttonId == R.id.three) {
        return KEY_FRAME_DURATION * 3;
      } else if (buttonId == R.id.four) {
        return KEY_FRAME_DURATION * 4;
      } else if (buttonId == R.id.five) {
        return KEY_FRAME_DURATION * 5;
      } else if (buttonId == R.id.six) {
        return KEY_FRAME_DURATION * 6;
      } else if (buttonId == R.id.seven) {
        return KEY_FRAME_DURATION * 7;
      } else if (buttonId == R.id.eight) {
        return KEY_FRAME_DURATION * 8;
      } else if (buttonId == R.id.nine) {
        return KEY_FRAME_DURATION * 9;
      } else if (buttonId == R.id.star) {
        return KEY_FRAME_DURATION * 10;
      } else if (buttonId == R.id.zero || buttonId == R.id.pound) {
        return KEY_FRAME_DURATION * 11;
      }
    }

    Log.wtf(TAG, "Attempted to get animation delay for invalid key button id.");
    return 0;
  }

  /**
   * Get the button animation duration, taking into account whether the dialpad is in landscape
   * left-to-right, landscape right-to-left, or portrait.
   *
   * @param buttonId The button ID.
   * @return The animation duration.
   */
  private int getKeyButtonAnimationDuration(int buttonId) {
    if (mIsLandscape) {
      if (mIsRtl) {
        if (buttonId == R.id.one
            || buttonId == R.id.four
            || buttonId == R.id.seven
            || buttonId == R.id.star) {
          return KEY_FRAME_DURATION * 8;
        } else if (buttonId == R.id.two
            || buttonId == R.id.five
            || buttonId == R.id.eight
            || buttonId == R.id.zero) {
          return KEY_FRAME_DURATION * 9;
        } else if (buttonId == R.id.three
            || buttonId == R.id.six
            || buttonId == R.id.nine
            || buttonId == R.id.pound) {
          return KEY_FRAME_DURATION * 10;
        }
      } else {
        if (buttonId == R.id.one
            || buttonId == R.id.four
            || buttonId == R.id.seven
            || buttonId == R.id.star) {
          return KEY_FRAME_DURATION * 10;
        } else if (buttonId == R.id.two
            || buttonId == R.id.five
            || buttonId == R.id.eight
            || buttonId == R.id.zero) {
          return KEY_FRAME_DURATION * 9;
        } else if (buttonId == R.id.three
            || buttonId == R.id.six
            || buttonId == R.id.nine
            || buttonId == R.id.pound) {
          return KEY_FRAME_DURATION * 8;
        }
      }
    } else {
      if (buttonId == R.id.one
          || buttonId == R.id.two
          || buttonId == R.id.three
          || buttonId == R.id.four
          || buttonId == R.id.five
          || buttonId == R.id.six) {
        return KEY_FRAME_DURATION * 10;
      } else if (buttonId == R.id.seven || buttonId == R.id.eight || buttonId == R.id.nine) {
        return KEY_FRAME_DURATION * 9;
      } else if (buttonId == R.id.star || buttonId == R.id.zero || buttonId == R.id.pound) {
        return KEY_FRAME_DURATION * 8;
      }
    }

    Log.wtf(TAG, "Attempted to get animation duration for invalid key button id.");
    return 0;
  }
}
