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

package im.vector.riotx.features.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.matrix.android.api.failure.ConsentNotGivenError
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.utils.LiveEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionListener @Inject constructor() : Session.Listener {

    private val _consentNotGivenLiveData = MutableLiveData<LiveEvent<ConsentNotGivenError>>()
    val consentNotGivenLiveData: LiveData<LiveEvent<ConsentNotGivenError>>
        get() = _consentNotGivenLiveData

    override fun onInvalidToken() {
        // TODO Handle this error
        Timber.e("Token is not valid anymore: handle this properly")
    }

    override fun onConsentNotGivenError(consentNotGivenError: ConsentNotGivenError) {
        _consentNotGivenLiveData.postLiveEvent(consentNotGivenError)
    }

}