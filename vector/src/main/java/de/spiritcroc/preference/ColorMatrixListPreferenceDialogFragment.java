/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2019 Android Ice Cold Project
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

package de.spiritcroc.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Arrays;

import im.vector.app.R;

import static de.spiritcroc.preference.ColorMatrixListPreference.COLOR_MAGIC_TEXT;


public class ColorMatrixListPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_VALUE = "ColorMatrixListPreferenceDialogFragment.value";
    private static final String SAVE_STATE_ENTRIES =
            "ColorMatrixListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "ColorMatrixListPreferenceDialogFragment.entryValues";
    private static final String SAVE_STATE_ENTRY_PREVIEWS =
            "ColorMatrixListPreferenceDialogFragment.entryPreviews";
    private static final String SAVE_STATE_ENTRY_PREVIEWS_LIGHT =
            "ColorMatrixListPreferenceDialogFragment.entryPreviewsLight";

    // TODO config?
    private int mColumnCount = 5;

    private String mValue;
    private boolean mPositiveResult = false;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private CharSequence[] mEntryPreviews;
    private CharSequence[] mEntryPreviewsLight;
    private ColorListEntry[] mColorListEntries;

    private boolean mExpertModePossible = false;
    private boolean mExpertMode = false;

    private LinearLayout mBaseLayout;

    public static ColorMatrixListPreferenceDialogFragment newInstance(String key) {
        final ColorMatrixListPreferenceDialogFragment fragment = new ColorMatrixListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            final ColorMatrixListPreference preference = (ColorMatrixListPreference) getPreference();

            if (preference.getEntryPreviews() == null) {
                throw new IllegalStateException(
                        "ColorMatrixListPreference requires an entryPreviews array.");
            }

            mValue = preference.getValue();
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
            mEntryPreviews = preference.getEntryPreviews();
            mEntryPreviewsLight = preference.getEntryPreviewsLight();
        } else {
            mValue = savedInstanceState.getString(SAVE_STATE_VALUE);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
            mEntryPreviews = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_PREVIEWS);
            mEntryPreviewsLight =
                    savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_PREVIEWS_LIGHT);
        }
        if (mEntryPreviewsLight == null) {
            mEntryPreviewsLight = mEntryPreviews;
        }
        buildColorArray();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVE_STATE_VALUE, mValue);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_PREVIEWS, mEntryPreviews);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_PREVIEWS_LIGHT, mEntryPreviewsLight);
    }

    private boolean isDarkTheme() {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        int bgColor = tv.data;
        getContext().getTheme().resolveAttribute(android.R.attr.colorForeground, tv, true);
        int fgColor = tv.data;
        return Color.luminance(fgColor) > Color.luminance(bgColor);
    }

    private int getAccentColor() {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, tv, true);
        return tv.data;
    }

    private int parseColor(CharSequence s) {
        int result = Integer.decode(s.toString());
        if ((result & 0xff000000) == 0) {
            // Add alpha channel
            result |= 0xff000000;
        }
        return result;
    }

    private void buildColorArray() {
        mColorListEntries = new ColorListEntry[mEntries.length];
        boolean isDark = isDarkTheme();
        for (int i = 0; i < mEntries.length; i++) {
            mColorListEntries[i] = new ColorListEntry(isDark, mEntries[i], mEntryValues[i],
                    parseColor(mEntryPreviews[i]), parseColor(mEntryPreviewsLight[i]));
            if (mColorListEntries[i].entryPreview != mColorListEntries[i].entryPreviewLight) {
                mExpertModePossible = true;
            }
        }
        Arrays.sort(mColorListEntries);
    }

    @Override
    protected View onCreateDialogView(Context context) {
        mBaseLayout = new LinearLayout(context);
        mBaseLayout.setOrientation(LinearLayout.VERTICAL);

        populateView(context);

        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        ScrollView view = new ScrollView(context);
        view.setLayoutParams(layoutParams);
        view.addView(mBaseLayout);
        return view;
    }

    private void rebuildView(Context context) {
        mBaseLayout.removeAllViews();
        populateView(context);
    }

    private void populateView(Context context) {
        int previewSize = getResources()
                .getDimensionPixelSize(R.dimen.color_matrix_list_preview_size);

        ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        LinearLayout.LayoutParams childLayoutParams =
                new LinearLayout.LayoutParams(0, previewSize);
        childLayoutParams.weight = 1;

        LinearLayout.LayoutParams subChild1LayoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        subChild1LayoutParams.weight = 1;

        LinearLayout.LayoutParams subChild2LayoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, previewSize/4);

        LinearLayout.LayoutParams firstTextChildLayoutParams =
                new LinearLayout.LayoutParams(0, previewSize);
        firstTextChildLayoutParams.weight = 2;

        for (int i = 0; i < mColorListEntries.length;) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setLayoutParams(layoutParams);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            mBaseLayout.addView(rowLayout);
            for (int j = 0; j < mColumnCount && i < mEntries.length; j++) {
                boolean selected = mColorListEntries[i].entryValue.toString().equals(mValue);
                View child;
                int color = mColorListEntries[i].getPreview();
                int color2 = mColorListEntries[i].getPreview2();
                if (color == COLOR_MAGIC_TEXT) {
                    TextView tv = new TextView(context);
                    tv.setText(mEntries[i]);
                    tv.setGravity(Gravity.CENTER);
                    child = tv;
                    if (i == 0) {
                        // If text view on first position: give it twice as much space
                        child.setLayoutParams(firstTextChildLayoutParams);
                        j++;
                    } else {
                        child.setLayoutParams(childLayoutParams);
                    }
                    if (selected) {
                        tv.setTextColor(getAccentColor());
                    }
                } else {
                    if (mExpertMode && color2 != color) {
                        // Two previews, with weight on the one chosen from the theme
                        LinearLayout childLayout = new LinearLayout(context);
                        childLayout.setOrientation(LinearLayout.VERTICAL);
                        View child1 = new View(context);
                        child1.setBackgroundColor(color);
                        child1.setLayoutParams(subChild1LayoutParams);
                        childLayout.addView(child1);
                        View child2 = new View(context);
                        child2.setBackgroundColor(color2);
                        child2.setLayoutParams(subChild2LayoutParams);
                        childLayout.addView(child2);
                        child = childLayout;
                    } else {
                        child = new View(context);
                        child.setBackgroundColor(color);
                    }

                    if (selected) {
                        int selectionPadding = getResources()
                                .getDimensionPixelSize(R.dimen.color_matrix_list_selection_padding);
                        FrameLayout.LayoutParams frameChildParams =
                                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT);
                        frameChildParams.setMargins(selectionPadding, selectionPadding,
                                selectionPadding, selectionPadding);
                        child.setLayoutParams(frameChildParams);
                        FrameLayout frame = new FrameLayout(context);
                        frame.addView(child);
                        child = frame;
                    }
                    child.setLayoutParams(childLayoutParams);
                }
                child.setOnClickListener(mChildClickListener);
                child.setTag(mColorListEntries[i]);
                rowLayout.addView(child);
                i++;
            }
        }
    }

    private View.OnClickListener mChildClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mValue = ((ColorListEntry) view.getTag()).entryValue.toString();
            mPositiveResult = true;
            dismiss();
        }
    };


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (mEntries == null || mEntryValues == null || mEntryPreviews == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array, an entryValues array, and an entryPreviews array.");
        }

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null);

        // Toggle to expert mode
        if (mExpertModePossible) {
            builder.setNeutralButton(R.string.color_matrix_list_expert_mode, null);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = (AlertDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (mExpertModePossible) {
                    final Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    neutralButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mExpertMode = !mExpertMode;
                            neutralButton.setText(mExpertMode
                                    ? R.string.color_matrix_list_complainer_mode
                                    : R.string.color_matrix_list_expert_mode);
                            rebuildView(getContext());
                        }
                    });
                }
            }
        });
        return dialog;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final ColorMatrixListPreference preference = (ColorMatrixListPreference) getPreference();
        if ((positiveResult||mPositiveResult) && mValue != null) {
            if (preference.callChangeListener(mValue)) {
                preference.setValue(mValue);
            }
            mPositiveResult = false;
        }
    }

    private class ColorListEntry implements Comparable<ColorListEntry> {
        private CharSequence entry;
        private CharSequence entryValue;
        private int entryPreview;
        private int entryPreviewLight;
        private boolean useDark;
        private ColorListEntry(boolean useDark, CharSequence entry, CharSequence entryValue,
                               int entryPreview, int entryPreviewLight) {
            this.useDark = useDark;
            this.entry = entry;
            this.entryValue = entryValue;
            this.entryPreview = entryPreview;
            this.entryPreviewLight = entryPreviewLight;
        }
        @Override
        public int compareTo(ColorListEntry other) {
            int result = compareColor(getPreview(), other.getPreview());
            if (result != 0) {
                return result;
            }
            return compareColor(getPreview2(), getPreview2());
        }
        private int compareColor(int a, int b) {
            float[] hsv = new float[3], otherHsv = new float[3];
            if (a == COLOR_MAGIC_TEXT) {
                // Magic value for text, add to beginning
                return -1;
            } else if (b == COLOR_MAGIC_TEXT) {
                // Magic value for text, add to beginning
                return 1;
            }
            Color.colorToHSV(a, hsv);
            Color.colorToHSV(b, otherHsv);
            if (hsv[0] == otherHsv[0]) {
                if (hsv[1] == otherHsv[1]) {
                    if (hsv[2] == otherHsv[2]) {
                        return 0;
                    }
                    return hsv[2] > otherHsv[2] ? 1 : -1;
                }
                return hsv[1] > otherHsv[1] ? 1 : -1;
            }
            return hsv[0] > otherHsv[0] ? 1 : -1;
        }
        private int getPreview() {
            return useDark ? entryPreview : entryPreviewLight;
        }
        private int getPreview2() {
            return useDark ? entryPreviewLight : entryPreview;
        }
    }
}
