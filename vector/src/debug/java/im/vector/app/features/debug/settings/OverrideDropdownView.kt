/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.debug.settings

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import im.vector.app.R
import im.vector.app.databinding.ViewBooleanDropdownBinding

class OverrideDropdownView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = ViewBooleanDropdownBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun <T : OverrideOption> bind(feature: OverrideDropdown<T>, listener: Listener<T>) {
        binding.overrideLabel.text = feature.label

        binding.overrideOptions.apply {
            val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item)
            val options = listOf("Inactive") + feature.options.map { it.label }
            arrayAdapter.addAll(options)
            adapter = arrayAdapter

            feature.activeOption?.let {
                setSelection(options.indexOf(it.label), false)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position) {
                        0    -> listener.onOverrideSelected(option = null)
                        else -> listener.onOverrideSelected(feature.options[position - 1])
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
        }
    }

    fun interface Listener<T> {
        fun onOverrideSelected(option: T?)
    }

    data class OverrideDropdown<T : OverrideOption>(
            val label: String,
            val options: List<T>,
            val activeOption: T?,
    )
}

interface OverrideOption {
    val label: String
}
