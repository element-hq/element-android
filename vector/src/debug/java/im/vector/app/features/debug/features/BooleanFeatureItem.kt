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
abstract class BooleanFeatureItem : VectorEpoxyModel<BooleanFeatureItem.Holder>() {

    @EpoxyAttribute
    lateinit var feature: Feature.BooleanFeature

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var listener: Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.label.text = feature.label

        holder.optionsSpinner.apply {
            val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item)
            val options = listOf(
                    "DEFAULT - ${feature.featureDefault.toEmoji()}",
                    "✅",
                    "❌"
            )
            arrayAdapter.addAll(options)
            adapter = arrayAdapter

            feature.featureOverride?.let {
                setSelection(options.indexOf(it.toEmoji()), false)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when (position) {
                        0    -> listener?.onBooleanOptionSelected(option = null, feature)
                        else -> listener?.onBooleanOptionSelected(options[position].fromEmoji(), feature)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val label by bind<TextView>(im.vector.app.R.id.feature_label)
        val optionsSpinner by bind<Spinner>(im.vector.app.R.id.feature_options)
    }

    interface Listener {
        fun onBooleanOptionSelected(option: Boolean?, feature: Feature.BooleanFeature)
    }
}

private fun Boolean.toEmoji() = if (this) "✅" else "❌"
private fun String.fromEmoji() = when (this) {
    "✅"  -> true
    "❌"  -> false
    else -> error("unexpected input $this")
}
