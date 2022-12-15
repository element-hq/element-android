/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.matrix.android.sdk.api.session.accountdata.SessionAccountDataService
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional

class FakeSessionAccountDataService : SessionAccountDataService by mockk(relaxed = true) {

    fun givenGetUserAccountDataEventReturns(type: String, content: Content?) {
        every { getUserAccountDataEvent(type) } returns content?.let { UserAccountDataEvent(type, it) }
    }

    fun givenGetLiveUserAccountDataEventReturns(type: String, content: Content?): LiveData<Optional<UserAccountDataEvent>> {
        return MutableLiveData(content?.let { UserAccountDataEvent(type, it) }.toOptional())
                .also {
                    every { getLiveUserAccountDataEvent(type) } returns it
                }
    }

    fun givenUpdateUserAccountDataEventSucceeds() {
        coEvery { updateUserAccountData(any(), any()) } just runs
    }

    fun givenUpdateUserAccountDataEventFailsWithError(error: Exception) {
        coEvery { updateUserAccountData(any(), any()) } throws error
    }

    fun verifyUpdateUserAccountDataEventSucceeds(type: String, content: Content, inverse: Boolean = false) {
        coVerify(inverse = inverse) { updateUserAccountData(type, content) }
    }

    fun givenGetUserAccountDataEventsStartWith(type: String, userAccountDataEventList: List<UserAccountDataEvent>) {
        every { getUserAccountDataEventsStartWith(type) } returns userAccountDataEventList
    }
}
