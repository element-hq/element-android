/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            instance.awaitTransaction(any<suspend (Realm) -> Any>())
        } coAnswers {
            secondArg<suspend (Realm) -> Any>().invoke(fakeRealm.instance)
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
