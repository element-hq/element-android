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
package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.pushers.PusherData
import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.matrix.android.internal.session.pushers.JsonPusher
import im.vector.matrix.sqldelight.session.PusherEntity
import javax.inject.Inject

internal class PushersMapper @Inject constructor() {

    fun map(push_key: String,
            kind: String?,
            app_id: String,
            app_display_name: String?,
            device_display_name: String?,
            profile_tag: String?,
            lang: String?,
            data_url: String?,
            data_format: String?,
            state: String): Pusher {
        return Pusher(
                pushKey = push_key,
                kind = kind ?: "",
                appId = app_id,
                appDisplayName = app_display_name,
                deviceDisplayName = device_display_name,
                profileTag = profile_tag,
                lang = lang,
                data = PusherData(data_url, data_format),
                state = PusherState.valueOf(state)
        )
    }

    fun map(pusher: JsonPusher, state: PusherState): PusherEntity {
        return PusherEntity.Impl(
                push_key = pusher.pushKey,
                kind = pusher.kind,
                app_id = pusher.appId,
                app_display_name = pusher.appDisplayName,
                device_display_name = pusher.deviceDisplayName,
                profile_tag = pusher.profileTag,
                lang = pusher.lang,
                data_url = pusher.data?.url,
                data_format = pusher.data?.format,
                state = state.name
        )
    }
}
