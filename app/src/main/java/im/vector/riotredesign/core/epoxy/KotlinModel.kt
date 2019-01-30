/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.core.epoxy

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.OnModelVisibilityStateChangedListener
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class KotlinModel(
        @LayoutRes private val layoutRes: Int
) : EpoxyModel<View>() {

    private var view: View? = null
    private var onBindCallback: (() -> Unit)? = null
    private var onModelVisibilityStateChangedListener: OnModelVisibilityStateChangedListener<KotlinModel, View>? = null

    abstract fun bind()

    override fun bind(view: View) {
        this.view = view
        onBindCallback?.invoke()
        bind()
    }

    override fun unbind(view: View) {
        this.view = null
    }

    fun onBind(lambda: (() -> Unit)?): KotlinModel {
        onBindCallback = lambda
        return this
    }

    override fun onVisibilityStateChanged(visibilityState: Int, view: View) {
        onModelVisibilityStateChangedListener?.onVisibilityStateChanged(this, view, visibilityState)
        super.onVisibilityStateChanged(visibilityState, view)
    }

    fun setOnVisibilityStateChanged(listener: OnModelVisibilityStateChangedListener<KotlinModel, View>): KotlinModel {
        this.onModelVisibilityStateChangedListener = listener
        return this
    }

    override fun getDefaultLayout() = layoutRes

    protected fun <V : View> bind(@IdRes id: Int) = object : ReadOnlyProperty<KotlinModel, V> {
        override fun getValue(thisRef: KotlinModel, property: KProperty<*>): V {
            // This is not efficient because it looks up the view by id every time (it loses
            // the pattern of a "holder" to cache that look up). But it is simple to use and could
            // be optimized with a map
            @Suppress("UNCHECKED_CAST")
            return view?.findViewById(id) as V?
                   ?: throw IllegalStateException("View ID $id for '${property.name}' not found.")
        }
    }
}