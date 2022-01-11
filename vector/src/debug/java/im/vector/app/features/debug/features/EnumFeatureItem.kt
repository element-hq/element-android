/*
 * Copyright 2019 New Vector Ltd
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

@EpoxyModelClass(layout = im.vector.app.R.layout.item_feature)
abstract class EnumFeatureItem : VectorEpoxyModel<EnumFeatureItem.Holder>() {

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
                        0    -> listener?.onEnumOptionSelected(option = null, feature)
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
        val label by bind<TextView>(im.vector.app.R.id.feature_label)
        val optionsSpinner by bind<Spinner>(im.vector.app.R.id.feature_options)
    }

    interface Listener {
        fun <T : Enum<T>> onEnumOptionSelected(option: T?, feature: Feature.EnumFeature<T>)
    }
}
