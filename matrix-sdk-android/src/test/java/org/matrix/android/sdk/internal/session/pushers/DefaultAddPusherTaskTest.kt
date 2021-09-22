/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.pushers

import com.zhuinden.monarchy.Monarchy
import io.mockk.MockKVerificationScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.kotlin.where
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.pushers.PusherState
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.util.awaitTransaction

private val A_JSON_PUSHER = JsonPusher(
        pushKey = "push-key",
        kind = "http",
        appId = "m.email",
        appDisplayName = "Element",
        deviceDisplayName = null,
        profileTag = "",
        lang = "en-GB",
        data = JsonPusherData(brand = "Element")
)

class DefaultAddPusherTaskTest {

    private val pushersAPI = FakePushersAPI()
    private val monarchy = FakeMonarchy()

    private val addPusherTask = DefaultAddPusherTask(
            pushersAPI = pushersAPI,
            monarchy = monarchy.instance,
            requestExecutor = FakeRequestExecutor(),
            globalErrorReceiver = FakeGlobalErrorReceiver()
    )

    @Test
    fun `given no persisted pusher when running task then fetches from api and inserts result with Registered state`() = runBlocking {
        monarchy.givenWhereReturns<PusherEntity>(result = null)

        addPusherTask.execute(AddPusherTask.Params(A_JSON_PUSHER))

        pushersAPI.verifySetPusher(A_JSON_PUSHER)
        monarchy.verifyInsertOrUpdate<PusherEntity> {
            withArg { actual ->
                actual.state shouldBeEqualTo PusherState.REGISTERED
            }
        }
    }
}

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

internal class FakeMonarchy {

    val instance = mockk<Monarchy>()
    private val realm = mockk<Realm>(relaxed = true)

    init {
        mockkStatic("org.matrix.android.sdk.internal.util.MonarchyKt")
        coEvery {
            instance.awaitTransaction(any<suspend (Realm) -> Any>())
        } coAnswers {
            secondArg<suspend (Realm) -> Any>().invoke(realm)
        }
    }

    inline fun <reified T : RealmModel> givenWhereReturns(result: T?) {
        val queryResult = mockk<RealmQuery<T>>(relaxed = true)
        every { queryResult.findFirst() } returns result
        every { realm.where<T>() } returns queryResult
    }

    inline fun <reified T : RealmModel> verifyInsertOrUpdate(crossinline verification: MockKVerificationScope.() -> T) {
        verify { realm.insertOrUpdate(verification()) }
    }
}

internal class FakeRequestExecutor : RequestExecutor {

    override suspend fun <DATA> executeRequest(globalErrorReceiver: GlobalErrorReceiver?,
                                               canRetry: Boolean,
                                               maxDelayBeforeRetry: Long,
                                               maxRetriesCount: Int,
                                               requestBlock: suspend () -> DATA): DATA {
        return requestBlock()
    }
}

internal class FakeGlobalErrorReceiver : GlobalErrorReceiver {
    override fun handleGlobalError(globalError: GlobalError) {
        // do nothing
    }
}
