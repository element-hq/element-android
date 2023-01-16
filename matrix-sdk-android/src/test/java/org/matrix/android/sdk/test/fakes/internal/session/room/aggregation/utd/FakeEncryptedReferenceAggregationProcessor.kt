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

package org.matrix.android.sdk.test.fakes.internal.session.room.aggregation.utd

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.session.room.aggregation.utd.EncryptedReferenceAggregationProcessor

internal class FakeEncryptedReferenceAggregationProcessor {

    val instance: EncryptedReferenceAggregationProcessor = mockk()

    fun givenHandleReturns(result: Boolean) {
        every { instance.handle(any(), any(), any(), any()) } returns result
    }

    fun verifyHandle(
            realm: Realm,
            event: Event,
            isLocalEcho: Boolean,
            relatedEventId: String?,
    ) {
        verify { instance.handle(realm, event, isLocalEcho, relatedEventId) }
    }
}
