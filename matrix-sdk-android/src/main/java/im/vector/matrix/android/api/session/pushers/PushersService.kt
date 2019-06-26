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
package im.vector.matrix.android.api.session.pushers

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import java.util.*


interface PushersService {

    /**
     * Refresh pushers from server state
     */
    fun refreshPushers()

    /**
     * Add a new HTTP pusher.
     *
     * @param pushkey           the pushkey
     * @param appId             the application id
     * @param profileTag        the profile tag
     * @param lang              the language
     * @param appDisplayName    a human-readable application name
     * @param deviceDisplayName a human-readable device name
     * @param url               the URL that should be used to send notifications
     * @param append            append the pusher
     * @param withEventIdOnly   true to limit the push content
     *
     * @return A work request uuid. Can be used to listen to the status
     * (LiveData<WorkInfo> status = workManager.getWorkInfoByIdLiveData(<UUID>))
     */
    fun addHttpPusher(pushkey: String,
                      appId: String,
                      profileTag: String,
                      lang: String,
                      appDisplayName: String,
                      deviceDisplayName: String,
                      url: String,
                      append: Boolean,
                      withEventIdOnly: Boolean): UUID


    fun removeHttpPusher(pushkey: String, appId: String, callback: MatrixCallback<Unit>)

    companion object {
        const val EVENT_ID_ONLY = "event_id_only"
    }

    fun livePushers(): LiveData<List<Pusher>>
}