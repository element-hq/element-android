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
import org.junit.Test
import org.matrix.android.sdk.test.fakes.FakeAddPusherTask
import org.matrix.android.sdk.test.fakes.FakeGetPushersTask
import org.matrix.android.sdk.test.fakes.FakeMonarchy
import org.matrix.android.sdk.test.fakes.FakeRemovePusherTask
import org.matrix.android.sdk.test.fakes.FakeTaskExecutor
import org.matrix.android.sdk.test.fakes.FakeTogglePusherTask
import org.matrix.android.sdk.test.fakes.FakeWorkManagerConfig
import org.matrix.android.sdk.test.fakes.FakeWorkManagerProvider
import org.matrix.android.sdk.test.fakes.internal.FakePushGatewayNotifyTask
import org.matrix.android.sdk.test.fixtures.PusherFixture

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPushersServiceTest {

    private val workManagerProvider = FakeWorkManagerProvider()
    private val monarchy = FakeMonarchy()
    private val sessionId = ""
    private val getPushersTask = FakeGetPushersTask()
    private val pushGatewayNotifyTask = FakePushGatewayNotifyTask()
    private val addPusherTask = FakeAddPusherTask()
    private val togglePusherTask = FakeTogglePusherTask()
    private val removePusherTask = FakeRemovePusherTask()
    private val taskExecutor = FakeTaskExecutor()
    private val fakeWorkManagerConfig = FakeWorkManagerConfig()

    private val pushersService = DefaultPushersService(
            workManagerProvider.instance,
            monarchy.instance,
            sessionId,
            getPushersTask,
            pushGatewayNotifyTask,
            addPusherTask,
            togglePusherTask,
            removePusherTask,
            taskExecutor.instance,
            fakeWorkManagerConfig,
    )

    @Test
    fun `when togglePusher, then execute task`() = runTest {
        val pusher = PusherFixture.aPusher()
        val enable = true

        pushersService.togglePusher(pusher, enable)

        togglePusherTask.verifyExecution(pusher, enable)
    }
}
