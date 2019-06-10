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

package im.vector.riotredesign.core.mvrx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.MvRxState
import im.vector.riotredesign.core.platform.VectorViewModel
import im.vector.riotredesign.core.utils.LiveEvent

// MvRx require a state with at least one attribute
data class NavigationState(val dummy: Boolean = false) : MvRxState

abstract class NavigationViewModel<NavigationClass>(initialState: NavigationState) : VectorViewModel<NavigationState>(initialState) {

    private val _navigateTo = MutableLiveData<LiveEvent<NavigationClass>>()
    val navigateTo: LiveData<LiveEvent<NavigationClass>>
        get() = _navigateTo


    fun goTo(navigation: NavigationClass) {
        _navigateTo.postValue(LiveEvent(navigation))
    }
}