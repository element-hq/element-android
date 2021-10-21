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

package org.matrix.android.sdk.test.fakes

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
import org.matrix.android.sdk.internal.util.awaitTransaction

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
