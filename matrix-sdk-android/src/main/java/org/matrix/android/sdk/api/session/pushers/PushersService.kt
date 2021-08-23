/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.api.session.pushers

import androidx.lifecycle.LiveData
import java.util.UUID

interface PushersService {

    /**
     * Refresh pushers from server state
     */
    fun refreshPushers()

    /**
     * Add a new HTTP pusher.
     * Note that only `http` kind is supported by the SDK for now.
     * Ref: https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-pushers-set
     *
     * @param pushkey           This is a unique identifier for this pusher. The value you should use for
     *                          this is the routing or destination address information for the notification,
     *                          for example, the APNS token for APNS or the Registration ID for GCM. If your
     *                          notification client has no such concept, use any unique identifier. Max length, 512 chars.
     *                          If the kind is "email", this is the email address to send notifications to.
     * @param appId             the application id
     *                          This is a reverse-DNS style identifier for the application. It is recommended
     *                          that this end with the platform, such that different platform versions get
     *                          different app identifiers. Max length, 64 chars.
     * @param profileTag        This string determines which set of device specific rules this pusher executes.
     * @param lang              The preferred language for receiving notifications (e.g. "en" or "en-US").
     * @param appDisplayName    A human readable string that will allow the user to identify what application owns this pusher.
     * @param deviceDisplayName A human readable string that will allow the user to identify what device owns this pusher.
     * @param url               The URL to use to send notifications to. MUST be an HTTPS URL with a path of /_matrix/push/v1/notify.
     * @param append            If true, the homeserver should add another pusher with the given pushkey and App ID in addition
     *                          to any others with different user IDs. Otherwise, the homeserver must remove any other pushers
     *                          with the same App ID and pushkey for different users.
     * @param withEventIdOnly   true to limit the push content to only id and not message content
     *                          Ref: https://matrix.org/docs/spec/push_gateway/r0.1.1#homeserver-behaviour
     *
     * @return                  A work request uuid. Can be used to listen to the status
     *                          (LiveData<WorkInfo> status = workManager.getWorkInfoByIdLiveData(<UUID>))
     * @throws [InvalidParameterException] if a parameter is not correct
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

    /**
     * Directly ask the push gateway to send a push to this device
     * If successful, the push gateway has accepted the request. In this case, the app should receive a Push with the provided eventId.
     * In case of error, PusherRejected will be thrown. In this case it means that the pushkey is not valid.
     *
     * @param url the push gateway url (full path)
     * @param appId the application id
     * @param pushkey the FCM token
     * @param eventId the eventId which will be sent in the Push message. Use a fake eventId.
     */
    suspend fun testPush(url: String,
                         appId: String,
                         pushkey: String,
                         eventId: String)

    /**
     * Remove the http pusher
     */
    suspend fun removeHttpPusher(pushkey: String, appId: String)

    /**
     * Get the current pushers, as a LiveData
     */
    fun getPushersLive(): LiveData<List<Pusher>>

    /**
     * Get the current pushers
     */
    fun getPushers(): List<Pusher>
}
