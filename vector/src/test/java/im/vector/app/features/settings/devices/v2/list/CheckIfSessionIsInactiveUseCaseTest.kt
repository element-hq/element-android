/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.test.fakes.FakeClock
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

private const val A_TIMESTAMP_MILLIS = 1654689143000L

class CheckIfSessionIsInactiveUseCaseTest {

    private val clock = FakeClock().apply { givenEpoch(A_TIMESTAMP_MILLIS) }
    private val checkIfSessionIsInactiveUseCase = CheckIfSessionIsInactiveUseCase(clock)

    @Test
    fun `given an old last seen date then session is inactive`() {
        val lastSeenDate = A_TIMESTAMP_MILLIS - TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong()) - 1

        val result = checkIfSessionIsInactiveUseCase.execute(lastSeenDate)

        result.shouldBeTrue()
    }

    @Test
    fun `given a last seen date equal to the threshold then session is inactive`() {
        val lastSeenDate = A_TIMESTAMP_MILLIS - TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong())

        val result = checkIfSessionIsInactiveUseCase.execute(lastSeenDate)

        result.shouldBeTrue()
    }

    @Test
    fun `given a recent last seen date then session is active`() {
        val lastSeenDate = A_TIMESTAMP_MILLIS - TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong()) + 1

        val result = checkIfSessionIsInactiveUseCase.execute(lastSeenDate)

        result.shouldBeFalse()
    }

    @Test
    fun `given a last seen date as zero then session is not inactive`() {
        val lastSeenDate = 0L

        val result = checkIfSessionIsInactiveUseCase.execute(lastSeenDate)

        result.shouldBeFalse()
    }

    @Test
    fun `given a last seen date as null then session is not inactive`() {
        val lastSeenDate = null

        val result = checkIfSessionIsInactiveUseCase.execute(lastSeenDate)

        result.shouldBeFalse()
    }

    @Test
    fun `given a last seen date as negative then session is not inactive`() {
        val lastSeenDate = -3L

        val result = checkIfSessionIsInactiveUseCase.execute(lastSeenDate)

        result.shouldBeFalse()
    }
}
