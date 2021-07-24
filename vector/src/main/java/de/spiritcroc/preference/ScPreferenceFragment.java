/*
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

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference;

public abstract class ScPreferenceFragment extends PreferenceFragmentCompat {

    private static final String DIALOG_FRAGMENT_TAG =
            "de.spiritcroc.preference.ScPreferenceFragment.DIALOG";

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // check if dialog is already showing
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        if (preference instanceof ColorMatrixListPreference) {
                ColorMatrixListPreferenceDialogFragment dialogFragment =
                        ColorMatrixListPreferenceDialogFragment.newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

}
