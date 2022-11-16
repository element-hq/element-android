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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zhuinden.monarchy.Monarchy
import io.mockk.MockKVerificationScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import org.matrix.android.sdk.internal.util.awaitTransaction

internal class FakeMonarchy {

    val instance = mockk<Monarchy>()
    val fakeRealm = FakeRealm()

    init {
        mockkStatic("org.matrix.android.sdk.internal.util.MonarchyKt")
        coEvery {
            instance.awaitTransaction(any<(Realm) -> Any>())
        } answers {
            secondArg<(Realm) -> Any>().invoke(fakeRealm.instance)
        }
        coEvery {
            instance.doWithRealm(any())
        } coAnswers {
            firstArg<Monarchy.RealmBlock>().doWithRealm(fakeRealm.instance)
        }
        coEvery {
            instance.runTransactionSync(any())
        } coAnswers {
            firstArg<Realm.Transaction>().execute(fakeRealm.instance)
        }
        every { instance.realmConfiguration } returns mockk()
    }

    inline fun <reified T : RealmModel> givenWhere(): RealmQuery<T> {
        return fakeRealm.givenWhere()
    }

    inline fun <reified T : RealmModel> givenWhereReturns(result: T?): RealmQuery<T> {
        return fakeRealm.givenWhere<T>()
                .givenFindFirst(result)
    }

    inline fun <reified T : RealmModel> verifyInsertOrUpdate(crossinline verification: MockKVerificationScope.() -> T) {
        fakeRealm.verifyInsertOrUpdate(verification)
    }

    inline fun <reified R, reified T : RealmModel> givenFindAllMappedWithChangesReturns(
            realmEntities: List<T>,
            mappedResult: List<R>,
            mapper: Monarchy.Mapper<R, T>
    ): LiveData<List<R>> {
        every { mapper.map(any()) } returns mockk()
        val monarchyQuery = slot<Monarchy.Query<T>>()
        val monarchyMapper = slot<Monarchy.Mapper<R, T>>()
        val result = MutableLiveData(mappedResult)
        every {
            instance.findAllMappedWithChanges(capture(monarchyQuery), capture(monarchyMapper))
        } answers {
            monarchyQuery.captured.createQuery(fakeRealm.instance)
            realmEntities.forEach {
                monarchyMapper.captured.map(it)
            }
            result
        }
        return result
    }
}
