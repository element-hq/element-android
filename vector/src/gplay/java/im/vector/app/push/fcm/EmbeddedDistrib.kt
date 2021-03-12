package im.vector.app.push.fcm

import android.content.Context
import im.vector.app.R
import org.unifiedpush.android.embedded_fcm_distributor.GetEndpointHandler
import org.unifiedpush.android.embedded_fcm_distributor.EmbeddedDistributorReceiver

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

val handlerFCM = object: GetEndpointHandler {
    override fun getEndpoint(context: Context?, token: String, instance: String): String {
        // This returns the endpoint of your FCM Rewrite-Proxy
        return "${context!!.getString(R.string.pusher_http_url)}FCM?instance=$instance&token=$token"
    }
}

class EmbeddedDistrib: EmbeddedDistributorReceiver(handlerFCM)
