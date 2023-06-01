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

package im.vector.app.core.device

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeCryptoService
import im.vector.app.test.fakes.FakeSession
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class DefaultGetDeviceInfoUseCaseTest {

    private val cryptoService = FakeCryptoService()
    private val session = FakeSession(fakeCryptoService = cryptoService)
    private val activeSessionHolder = FakeActiveSessionHolder(session)

    private val getDeviceInfoUseCase = DefaultGetDeviceInfoUseCase(activeSessionHolder.instance)

    @Test
    fun `when execute, then get crypto device info`() = runTest {
        val result = getDeviceInfoUseCase.execute()

        result shouldBeEqualTo cryptoService.cryptoDeviceInfo
    }
}
