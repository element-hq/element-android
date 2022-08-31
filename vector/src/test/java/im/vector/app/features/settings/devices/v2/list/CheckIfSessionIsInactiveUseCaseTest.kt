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

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.core.resources.DateProvider
import im.vector.app.core.resources.toTimestamp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneId.systemDefault
import org.threeten.bp.ZoneOffset
import org.threeten.bp.zone.ZoneRules

class CheckIfSessionIsInactiveUseCaseTest {

    private val checkIfSessionIsInactiveUseCase = CheckIfSessionIsInactiveUseCase()
    private val zoneId = mockk<ZoneId>()
    private val zoneRules = mockk<ZoneRules>()

    @Before
    fun setup() {
        mockkStatic(ZoneId::class)
        every { systemDefault() } returns zoneId
        every { zoneId.rules } returns zoneRules
        every { zoneRules.getOffset(any<Instant>()) } returns ZoneOffset.UTC
        every { zoneRules.getOffset(any<LocalDateTime>()) } returns ZoneOffset.UTC
    }

    @Test
    fun `given an old last seen date then session is inactive`() {
        val lastSeenDate = DateProvider.currentLocalDateTime().minusDays((SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS + 1).toLong())

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate.toTimestamp()) shouldBeEqualTo true
    }

    @Test
    fun `given a last seen date equal to the threshold then session is inactive`() {
        val lastSeenDate = DateProvider.currentLocalDateTime().minusDays((SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS).toLong())

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate.toTimestamp()) shouldBeEqualTo true
    }

    @Test
    fun `given a recent last seen date then session is active`() {
        val lastSeenDate = DateProvider.currentLocalDateTime().minusDays((SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS - 1).toLong())

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate.toTimestamp()) shouldBeEqualTo false
    }

    @Test
    fun `given a last seen date as zero then session is inactive`() {
        // In case of the server doesn't send the last seen date.
        val lastSeenDate = DateProvider.toLocalDateTime(0)

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate.toTimestamp()) shouldBeEqualTo true
    }
}
