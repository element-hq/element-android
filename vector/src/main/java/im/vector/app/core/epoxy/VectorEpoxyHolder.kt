/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.view.View
import com.airbnb.epoxy.EpoxyHolder
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A pattern for easier view binding with an [EpoxyHolder]
 *
 * See [SampleKotlinModelWithHolder] for a usage example.
 */
abstract class VectorEpoxyHolder : EpoxyHolder() {
    lateinit var view: View

    override fun bindView(itemView: View) {
        view = itemView
    }

    protected fun <V : View> bind(id: Int): ReadOnlyProperty<VectorEpoxyHolder, V> =
            Lazy { holder: VectorEpoxyHolder, prop ->
                holder.view.findViewById(id) as V?
                        ?: throw IllegalStateException("View ID $id for '${prop.name}' not found.")
            }

    /**
     * Taken from Kotterknife.
     * https://github.com/JakeWharton/kotterknife
     */
    private class Lazy<V>(
            private val initializer: (VectorEpoxyHolder, KProperty<*>) -> V
    ) : ReadOnlyProperty<VectorEpoxyHolder, V> {
        private object EMPTY

        private var value: Any? = EMPTY

        override fun getValue(thisRef: VectorEpoxyHolder, property: KProperty<*>): V {
            if (value == EMPTY) {
                value = initializer(thisRef, property)
            }
            @Suppress("UNCHECKED_CAST")
            return value as V
        }
    }
}
