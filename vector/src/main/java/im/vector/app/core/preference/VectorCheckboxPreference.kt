/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder

class VectorCheckboxPreference : CheckBoxPreference {
    // Note: @JvmOverload does not work here...
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    var summaryTextColor: Int? = null

    init {
        // Set to false to remove the space when there is no icon
        isIconSpaceReserved = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        // display the title in multi-line to avoid ellipsis.
        (holder.findViewById(android.R.id.title) as? TextView)?.isSingleLine = false
        super.onBindViewHolder(holder)
    }

    override fun syncSummaryView(holder: PreferenceViewHolder) {
        super.syncSummaryView(holder)
        summaryTextColor?.let { (holder.findViewById(android.R.id.summary) as? TextView)?.setTextColor(it) }
    }
}
