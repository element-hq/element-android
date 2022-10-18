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

package org.matrix.android.sdk.internal.session.pushers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.model.PusherEntityFields
import org.matrix.android.sdk.test.fakes.FakeGlobalErrorReceiver
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakePushersAPI
import org.matrix.android.sdk.test.fakes.FakeRequestExecutor
import org.matrix.android.sdk.test.fakes.givenEqualTo
import org.matrix.android.sdk.test.fakes.givenFindFirst
import org.matrix.android.sdk.test.fixtures.JsonPusherFixture.aJsonPusher
import org.matrix.android.sdk.test.fixtures.PusherEntityFixture.aPusherEntity

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTogglePusherTaskTest {

    private val pushersAPI = FakePushersAPI()
    private val monarchy = FakeMonarchy()
    private val requestExecutor = FakeRequestExecutor()
    private val globalErrorReceiver = FakeGlobalErrorReceiver()

    private val togglePusherTask = DefaultTogglePusherTask(pushersAPI, monarchy.instance, requestExecutor, globalErrorReceiver)

    @Test
    fun `execution toggles enable on both local and remote`() = runTest {
        val jsonPusher = aJsonPusher(enabled = false)
        val params = TogglePusherTask.Params(aJsonPusher(), true)

        val pusherEntity = aPusherEntity(enabled = false)
        monarchy.givenWhere<PusherEntity>()
                .givenEqualTo(PusherEntityFields.PUSH_KEY, jsonPusher.pushKey)
                .givenFindFirst(pusherEntity)

        togglePusherTask.execute(params)

        val expectedPayload = jsonPusher.copy(enabled = true)
        pushersAPI.verifySetPusher(expectedPayload)
        monarchy.verifyInsertOrUpdate<PusherEntity> {
            withArg { actual ->
                actual.enabled shouldBeEqualTo true
            }
        }
    }
}
