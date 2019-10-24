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
package im.vector.riotx.features.home.room.detail.timeline.action

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.utils.LiveEvent
import javax.inject.Inject

/**
 * Activity shared view model to handle message actions
 */
class ActionsHandler @Inject constructor() : ViewModel() {

    val actionCommandEvent = MutableLiveData<LiveEvent<SimpleAction>>()

    fun fireAction(action: SimpleAction) {
        actionCommandEvent.postLiveEvent(action)
    }
}
