/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.utils

import org.amshove.kluent.shouldBe
import org.junit.Test
import java.lang.Thread.sleep

class TemporaryStoreTest {

    @Test
    fun testTemporaryStore() {
        // Keep the data 30 millis
        val store = TemporaryStore<String>(30)

        store.data = "test"
        store.data shouldBe "test"
        sleep(15)
        store.data shouldBe "test"
        sleep(20)
        store.data shouldBe null
    }
}
