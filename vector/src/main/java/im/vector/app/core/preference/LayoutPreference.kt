/*
 * Copyright (c) 2021 New Vector Ltd
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
import android.widget.RadioGroup
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import im.vector.app.R
import im.vector.app.features.settings.LayoutMode

class LayoutPreference : Preference {

    interface LayoutModeChangeListener {
        fun onLayoutModeChange(mode: LayoutMode)
    }

    var listener: LayoutModeChangeListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private var radioGroup: RadioGroup? = null

    init {
        layoutResource = R.layout.preference_app_choose_layout
        isIconSpaceReserved = true
    }

    var selectedLayoutMode: LayoutMode = LayoutMode.PRODUCTIVITY
        set(value) {
            when (value) {
                LayoutMode.SIMPLE       -> {
                    radioGroup?.check(R.id.simpleLayoutRadioButton)
                }
                LayoutMode.PRODUCTIVITY -> {
                    radioGroup?.check(R.id.proLayoutRadioButton)
                }
            }
            field = value
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.itemView?.setOnClickListener(null)

        radioGroup = holder?.findViewById(R.id.chooseLayoutRadioGroup) as? RadioGroup
        // set initial
        when (selectedLayoutMode) {
            LayoutMode.SIMPLE       -> {
                radioGroup?.check(R.id.simpleLayoutRadioButton)
            }
            LayoutMode.PRODUCTIVITY -> {
                radioGroup?.check(R.id.proLayoutRadioButton)
            }
        }

        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.simpleLayoutRadioButton -> listener?.onLayoutModeChange(LayoutMode.SIMPLE)
                R.id.proLayoutRadioButton    -> listener?.onLayoutModeChange(LayoutMode.PRODUCTIVITY)
                else                         -> {
                    // nop
                }
            }
        }
    }
}
