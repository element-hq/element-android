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

package androidx.lifecycle

/**
 * Manual test override to stop BaseMvRxViewModel from interacting with the android looper/main thread
 * Tests will run on their original test worker threads
 *
 * This has been fixed is newer versions of Mavericks via LifecycleRegistry.createUnsafe
 * https://github.com/airbnb/mavericks/blob/master/mvrx-rxjava2/src/main/kotlin/com/airbnb/mvrx/BaseMvRxViewModel.kt#L61
 */
@Suppress("UNUSED")
class LifecycleRegistry(@Suppress("UNUSED_PARAMETER") lifecycleOwner: LifecycleOwner) : Lifecycle() {

    private var state = State.INITIALIZED

    fun setCurrentState(state: State) {
        this.state = state
    }

    override fun addObserver(observer: LifecycleObserver) {
        TODO("Not yet implemented")
    }

    override fun removeObserver(observer: LifecycleObserver) {
        TODO("Not yet implemented")
    }

    override fun getCurrentState() = state
}
