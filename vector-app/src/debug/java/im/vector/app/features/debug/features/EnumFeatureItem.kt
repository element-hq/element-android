/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.features

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.application.R

@EpoxyModelClass
abstract class EnumFeatureItem : VectorEpoxyModel<EnumFeatureItem.Holder>(R.layout.item_feature) {

    @EpoxyAttribute
    lateinit var feature: Feature.EnumFeature<*>

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var listener: Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.label.text = feature.label

        holder.optionsSpinner.apply {
            val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item)
            arrayAdapter.add("DEFAULT - ${feature.default.name}")
            arrayAdapter.addAll(feature.options.map { it.name })
            adapter = arrayAdapter

            feature.override?.let {
                setSelection(feature.options.indexOf(it) + 1, false)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position) {
                        0 -> listener?.onEnumOptionSelected(option = null, feature)
                        else -> feature.onOptionSelected(position - 1)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
        }
    }

    private fun <T : Enum<T>> Feature.EnumFeature<T>.onOptionSelected(selection: Int) {
        listener?.onEnumOptionSelected(options[selection], this)
    }

    class Holder : VectorEpoxyHolder() {
        val label by bind<TextView>(R.id.feature_label)
        val optionsSpinner by bind<Spinner>(R.id.feature_options)
    }

    interface Listener {
        fun <T : Enum<T>> onEnumOptionSelected(option: T?, feature: Feature.EnumFeature<T>)
    }
}
