/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.preference

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import im.vector.app.R
import im.vector.app.features.themes.ThemeUtils

/**
 * Customize PreferenceCategory class to redefine some attributes.
 */
class VectorPreferenceCategory : PreferenceCategory {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        // Set to false to remove the space when there is no icon
        isIconSpaceReserved = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val titleTextView = holder.findViewById(android.R.id.title) as? TextView

        titleTextView?.setTypeface(null, Typeface.BOLD)
        titleTextView?.setTextColor(ThemeUtils.getColor(context, R.attr.vctr_content_primary))

        // "isIconSpaceReserved = false" does not work for preference category, so remove the padding
        if (!isIconSpaceReserved) {
            (titleTextView?.parent as? ViewGroup)?.setPadding(0, 0, 0, 0)
        }
    }
}
