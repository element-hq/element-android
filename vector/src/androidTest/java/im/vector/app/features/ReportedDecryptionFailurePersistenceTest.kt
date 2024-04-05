/*
 * Copyright (c) 2024 New Vector Ltd
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

package im.vector.app.features

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.InstrumentedTest
import im.vector.app.features.analytics.ReportedDecryptionFailurePersistence
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportedDecryptionFailurePersistenceTest : InstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun shouldPersistReportedUtds() = runTest {
        val persistence = ReportedDecryptionFailurePersistence(context)
        persistence.load()

        val eventIds = listOf("$0000", "$0001", "$0002", "$0003")
        eventIds.forEach {
            persistence.markAsReported(it)
        }

        eventIds.forEach {
            persistence.hasBeenReported(it) shouldBeEqualTo true
        }

        persistence.hasBeenReported("$0004") shouldBeEqualTo false

        persistence.persist()

        // Load a new one
        val persistence2 = ReportedDecryptionFailurePersistence(context)
        persistence2.load()

        eventIds.forEach {
            persistence2.hasBeenReported(it) shouldBeEqualTo true
        }
    }

    @Test
    fun testSaturation() = runTest {
        val persistence = ReportedDecryptionFailurePersistence(context)

        for  (i in 1..6000) {
            persistence.markAsReported("000$i")
        }

        // This should have saturated the bloom filter, making the rate of false positives too high.
        // A new bloom filter should have been created to avoid that and the recent reported events should still be in the new filter.
        for  (i in 5800..6000) {
            persistence.hasBeenReported("000$i") shouldBeEqualTo true
        }

        // Old ones should not be there though
        for  (i in 1..1000) {
            persistence.hasBeenReported("000$i") shouldBeEqualTo false
        }
    }
}
