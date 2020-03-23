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

package im.vector.riotx.features.home

import androidx.lifecycle.MutableLiveData
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.util.NoOpCancellable
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.singleBuilder
import im.vector.riotx.core.platform.VectorSharedActionViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class HomeSharedActionViewModel @Inject constructor() : VectorSharedActionViewModel<HomeActivitySharedAction>() {
    var hasDisplayedCompleteSecurityPrompt : Boolean = false
}
