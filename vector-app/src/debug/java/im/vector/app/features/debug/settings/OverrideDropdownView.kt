/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.application.databinding.ViewBooleanDropdownBinding

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
                        0 -> listener.onOverrideSelected(option = null)
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
