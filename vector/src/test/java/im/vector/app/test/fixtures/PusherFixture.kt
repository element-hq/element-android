/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.api.session.pushers.PusherData
import org.matrix.android.sdk.api.session.pushers.PusherState

object PusherFixture {

    fun aPusher(
            pushKey: String = "",
            kind: String = "",
            appId: String = "",
            appDisplayName: String? = "",
            deviceDisplayName: String? = "",
            profileTag: String? = null,
            lang: String? = "",
            data: PusherData = PusherData("f.o/_matrix/push/v1/notify", ""),
            enabled: Boolean = true,
            deviceId: String? = "",
            state: PusherState = PusherState.REGISTERED,
    ) = Pusher(
            pushKey,
            kind,
            appId,
            appDisplayName,
            deviceDisplayName,
            profileTag,
            lang,
            data,
            enabled,
            deviceId,
            state,
    )
}
