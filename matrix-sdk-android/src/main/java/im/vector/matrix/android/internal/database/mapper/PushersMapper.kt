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
import im.vector.matrix.android.internal.database.model.PusherDataEntity
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.session.pushers.JsonPusher

internal object PushersMapper {

    fun map(pushEntity: PusherEntity): Pusher {

        return Pusher(
                userId = pushEntity.userId,
                pushKey = pushEntity.pushKey,
                kind = pushEntity.kind ?: "",
                appId = pushEntity.appId,
                appDisplayName = pushEntity.appDisplayName,
                deviceDisplayName = pushEntity.deviceDisplayName,
                profileTag = pushEntity.profileTag,
                lang = pushEntity.lang,
                data = PusherData(pushEntity.data?.url, pushEntity.data?.format),
                state = pushEntity.state
        )
    }

    fun map(pusher: JsonPusher, userId: String): PusherEntity {
        return PusherEntity(
                userId = userId,
                pushKey = pusher.pushKey,
                kind = pusher.kind,
                appId = pusher.appId,
                appDisplayName = pusher.appDisplayName,
                deviceDisplayName = pusher.deviceDisplayName,
                profileTag = pusher.profileTag,
                lang = pusher.lang,
                data = PusherDataEntity(pusher.data?.url, pusher.data?.format)
        )
    }
}

internal fun PusherEntity.asDomain(): Pusher {
    return PushersMapper.map(this)
}

internal fun JsonPusher.toEntity(userId: String): PusherEntity {
    return PushersMapper.map(this, userId)
}