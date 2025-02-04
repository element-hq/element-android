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
abstract class BooleanFeatureItem : VectorEpoxyModel<BooleanFeatureItem.Holder>(R.layout.item_feature) {

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
                        0 -> listener?.onBooleanOptionSelected(option = null, feature)
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
        val label by bind<TextView>(R.id.feature_label)
        val optionsSpinner by bind<Spinner>(R.id.feature_options)
    }

    interface Listener {
        fun onBooleanOptionSelected(option: Boolean?, feature: Feature.BooleanFeature)
    }
}

private fun Boolean.toEmoji() = if (this) "✅" else "❌"
private fun String.fromEmoji() = when (this) {
    "✅" -> true
    "❌" -> false
    else -> error("unexpected input $this")
}
