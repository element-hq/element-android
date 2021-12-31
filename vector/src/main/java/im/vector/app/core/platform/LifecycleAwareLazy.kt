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

package im.vector.app.core.platform

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

fun <T> LifecycleOwner.lifecycleAwareLazy(initializer: () -> T): Lazy<T> = LifecycleAwareLazy(this, initializer)

private object UninitializedValue

class LifecycleAwareLazy<out T>(
        private val owner: LifecycleOwner,
        initializer: () -> T
) : Lazy<T>, DefaultLifecycleObserver {

    private var initializer: (() -> T)? = initializer

    private var _value: Any? = UninitializedValue

    @Suppress("UNCHECKED_CAST")
    override val value: T
        @MainThread
        get() {
            if (_value === UninitializedValue) {
                _value = initializer!!()
                attachToLifecycle()
            }
            return _value as T
        }

    override fun onDestroy(owner: LifecycleOwner) {
        _value = UninitializedValue
        detachFromLifecycle()
    }

    private fun attachToLifecycle() {
        if (getLifecycleOwner().lifecycle.currentState == Lifecycle.State.DESTROYED) {
            throw IllegalStateException("Initialization failed because lifecycle has been destroyed!")
        }
        getLifecycleOwner().lifecycle.addObserver(this)
    }

    private fun detachFromLifecycle() {
        getLifecycleOwner().lifecycle.removeObserver(this)
    }

    private fun getLifecycleOwner() = when (owner) {
        is Fragment -> owner.viewLifecycleOwner
        else        -> owner
    }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}
