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

package im.vector.riotx.core.mvrx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.utils.LiveEvent

abstract class NavigationViewModel<NavigationClass> : ViewModel() {

    private val _navigateTo = MutableLiveData<LiveEvent<NavigationClass>>()
    val navigateTo: LiveData<LiveEvent<NavigationClass>>
        get() = _navigateTo


    fun goTo(navigation: NavigationClass) {
        _navigateTo.postLiveEvent(navigation)
    }
}