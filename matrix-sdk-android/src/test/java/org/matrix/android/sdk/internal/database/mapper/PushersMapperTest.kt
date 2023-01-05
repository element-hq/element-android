/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.mapper

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.test.fixtures.JsonPusherFixture.aJsonPusher
import org.matrix.android.sdk.test.fixtures.PusherEntityFixture.aPusherEntity

class PushersMapperTest {

    @Test
    fun `when mapping PusherEntity, then it is mapped into Pusher successfully`() {
        val pusherEntity = aPusherEntity()

        val mappedPusher = PushersMapper.map(pusherEntity)

        mappedPusher.pushKey shouldBeEqualTo pusherEntity.pushKey
        mappedPusher.kind shouldBeEqualTo pusherEntity.kind.orEmpty()
        mappedPusher.appId shouldBeEqualTo pusherEntity.appId
        mappedPusher.appDisplayName shouldBeEqualTo pusherEntity.appDisplayName
        mappedPusher.deviceDisplayName shouldBeEqualTo pusherEntity.deviceDisplayName
        mappedPusher.profileTag shouldBeEqualTo pusherEntity.profileTag
        mappedPusher.lang shouldBeEqualTo pusherEntity.lang
        mappedPusher.data.url shouldBeEqualTo pusherEntity.data?.url
        mappedPusher.data.format shouldBeEqualTo pusherEntity.data?.format
        mappedPusher.enabled shouldBeEqualTo pusherEntity.enabled
        mappedPusher.deviceId shouldBeEqualTo pusherEntity.deviceId
        mappedPusher.state shouldBeEqualTo pusherEntity.state
    }

    @Test
    fun `when mapping JsonPusher, then it is mapped into Pusher successfully`() {
        val jsonPusher = aJsonPusher()

        val mappedPusherEntity = PushersMapper.map(jsonPusher)

        mappedPusherEntity.pushKey shouldBeEqualTo jsonPusher.pushKey
        mappedPusherEntity.kind shouldBeEqualTo jsonPusher.kind
        mappedPusherEntity.appId shouldBeEqualTo jsonPusher.appId
        mappedPusherEntity.appDisplayName shouldBeEqualTo jsonPusher.appDisplayName
        mappedPusherEntity.deviceDisplayName shouldBeEqualTo jsonPusher.deviceDisplayName
        mappedPusherEntity.profileTag shouldBeEqualTo jsonPusher.profileTag
        mappedPusherEntity.lang shouldBeEqualTo jsonPusher.lang
        mappedPusherEntity.data?.url shouldBeEqualTo jsonPusher.data?.url
        mappedPusherEntity.data?.format shouldBeEqualTo jsonPusher.data?.format
        mappedPusherEntity.enabled shouldBeEqualTo jsonPusher.enabled
        mappedPusherEntity.deviceId shouldBeEqualTo jsonPusher.deviceId
    }
}
