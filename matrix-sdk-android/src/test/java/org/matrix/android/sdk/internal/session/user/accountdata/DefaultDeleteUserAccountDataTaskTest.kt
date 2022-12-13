/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.user.accountdata

import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.test.fakes.FakeAccountDataApi
import org.matrix.android.sdk.test.fakes.FakeGlobalErrorReceiver

private const val A_TYPE = "a-type"
private const val A_USER_ID = "a-user-id"

@ExperimentalCoroutinesApi
class DefaultDeleteUserAccountDataTaskTest {

    private val fakeGlobalErrorReceiver = FakeGlobalErrorReceiver()
    private val fakeAccountDataApi = FakeAccountDataApi()

    private val deleteUserAccountDataTask = DefaultDeleteUserAccountDataTask(
            accountDataApi = fakeAccountDataApi.instance,
            userId = A_USER_ID,
            globalErrorReceiver = fakeGlobalErrorReceiver
    )

    @Test
    fun `given parameters when executing the task then api is called`() = runTest {
        // Given
        val params = DeleteUserAccountDataTask.Params(type = A_TYPE)
        fakeAccountDataApi.givenParamsToDeleteAccountData(A_USER_ID, A_TYPE)

        // When
        deleteUserAccountDataTask.execute(params)

        // Then
        coVerify { fakeAccountDataApi.instance.deleteAccountData(A_USER_ID, A_TYPE) }
    }
}
