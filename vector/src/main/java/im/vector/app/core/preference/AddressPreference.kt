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
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.preference.PreferenceViewHolder
import im.vector.app.R

/**
 * Preference used in Room setting for Room aliases
 */
class AddressPreference : VectorPreference {

    // members
    private var mMainAddressIconView: ImageView? = null
    private var mIsMainIconVisible = false

    /**
     * @return the main icon view.
     */
    val mainIconView: View?
        get() = mMainAddressIconView

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        widgetLayoutResource = R.layout.vector_settings_address_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val view = holder.itemView
        mMainAddressIconView = view.findViewById(R.id.main_address_icon_view)
        mMainAddressIconView!!.visibility = if (mIsMainIconVisible) View.VISIBLE else View.GONE
    }

    /**
     * Set the main address icon visibility.
     *
     * @param isVisible true to display the main icon
     */
    fun setMainIconVisible(isVisible: Boolean) {
        mIsMainIconVisible = isVisible

        mMainAddressIconView?.visibility = if (mIsMainIconVisible) View.VISIBLE else View.GONE
    }
}
