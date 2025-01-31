/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import org.amshove.kluent.shouldBeEqualTo
import org.matrix.android.sdk.internal.session.pushers.GetPushersResponse
import org.matrix.android.sdk.internal.session.pushers.JsonPusher
import org.matrix.android.sdk.internal.session.pushers.PushersAPI

internal class FakePushersAPI : PushersAPI {

    private var setRequestPayload: JsonPusher? = null
    private var error: Throwable? = null

    override suspend fun getPushers(): GetPushersResponse {
        TODO("Not yet implemented")
    }

    override suspend fun setPusher(jsonPusher: JsonPusher) {
        error?.let { throw it }
        setRequestPayload = jsonPusher
    }

    fun verifySetPusher(payload: JsonPusher) {
        this.setRequestPayload shouldBeEqualTo payload
    }

    fun givenSetPusherErrors(error: Throwable) {
        this.error = error
    }
}
