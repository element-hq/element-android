/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.api.session.pushers.PusherData
import org.matrix.android.sdk.internal.database.model.PusherDataEntity
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.session.pushers.JsonPusher

internal object PushersMapper {

    fun map(pushEntity: PusherEntity): Pusher {
        return Pusher(
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

    fun map(pusher: JsonPusher): PusherEntity {
        return PusherEntity(
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

internal fun JsonPusher.toEntity(): PusherEntity {
    return PushersMapper.map(this)
}
