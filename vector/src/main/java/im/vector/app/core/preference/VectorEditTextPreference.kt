/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder
import im.vector.app.R
import timber.log.Timber

/**
 * Use this class to create an EditTextPreference form code and avoid a crash (see https://code.google.com/p/android/issues/detail?id=231576)
 */
class VectorEditTextPreference : EditTextPreference {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        dialogLayoutResource = R.layout.dialog_preference_edit_text
        // Set to false to remove the space when there is no icon
        isIconSpaceReserved = true
    }

    // No single line for title
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        // display the title in multi-line to avoid ellipsis.
        try {
            (holder.findViewById(android.R.id.title) as? TextView)?.isSingleLine = false
        } catch (e: Exception) {
            Timber.e(e, "onBindView")
        }

        super.onBindViewHolder(holder)
    }
}
