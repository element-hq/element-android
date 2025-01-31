/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.test.fakes.FakeClock
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import java.util.concurrent.TimeUnit

private const val A_TIMESTAMP = 1654689143L

class CheckIfSessionIsInactiveUseCaseTest {

    private val clock = FakeClock().apply { givenEpoch(A_TIMESTAMP) }
    private val checkIfSessionIsInactiveUseCase = CheckIfSessionIsInactiveUseCase(clock)

    @Test
    fun `given an old last seen date then session is inactive`() {
        val lastSeenDate = A_TIMESTAMP - TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong()) - 1

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate) shouldBeEqualTo true
    }

    @Test
    fun `given a last seen date equal to the threshold then session is inactive`() {
        val lastSeenDate = A_TIMESTAMP - TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong())

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate) shouldBeEqualTo true
    }

    @Test
    fun `given a recent last seen date then session is active`() {
        val lastSeenDate = A_TIMESTAMP - TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong()) + 1

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate) shouldBeEqualTo false
    }

    @Test
    fun `given a last seen date as zero then session is inactive`() {
        // In case of the server doesn't send the last seen date.
        val lastSeenDate = 0L

        checkIfSessionIsInactiveUseCase.execute(lastSeenDate) shouldBeEqualTo true
    }
}
