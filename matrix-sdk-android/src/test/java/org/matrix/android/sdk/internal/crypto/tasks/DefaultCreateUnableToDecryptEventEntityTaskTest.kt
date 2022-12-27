/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.tasks

import io.mockk.unmockkAll
import io.mockk.verify
import io.realm.RealmModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.internal.database.model.UnableToDecryptEventEntity
import org.matrix.android.sdk.test.fakes.FakeRealm
import org.matrix.android.sdk.test.fakes.FakeRealmConfiguration

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultCreateUnableToDecryptEventEntityTaskTest {

    private val fakeRealmConfiguration = FakeRealmConfiguration()

    private val defaultCreateUnableToDecryptEventEntityTask = DefaultCreateUnableToDecryptEventEntityTask(
            realmConfiguration = fakeRealmConfiguration.instance,
    )

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an event id when execute then insert entity into database`() = runTest {
        // Given
        val anEventId = "event-id"
        val params = CreateUnableToDecryptEventEntityTask.Params(
                eventId = anEventId,
        )
        val fakeRealm = FakeRealm()
        fakeRealm.givenExecuteTransactionAsync()
        fakeRealmConfiguration.givenGetRealmInstance(fakeRealm.instance)

        // When
        defaultCreateUnableToDecryptEventEntityTask.execute(params)

        // Then
        verify {
            fakeRealm.instance.insert(match<RealmModel> {
                it is UnableToDecryptEventEntity && it.eventId == anEventId
            })
        }
    }
}
