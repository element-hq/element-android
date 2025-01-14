/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import org.amshove.kluent.shouldBe
import org.junit.Test
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.milliseconds

class TemporaryStoreTest {

    @Test
    fun testTemporaryStore() {
        // Keep the data 300 millis
        val store = TemporaryStore<String>(300.milliseconds)

        store.data = "test"
        store.data shouldBe "test"
        sleep(100)
        store.data shouldBe "test"
        sleep(300)
        store.data shouldBe null
    }
}
