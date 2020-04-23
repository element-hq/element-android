/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.matrix.sqldelight.session

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.junit.Test

class FilterTests : SessionTests {

    @Test
    fun test_all_operations() {
        val database = sessionDatabase()
        database.filterQueries.get().executeAsOneOrNull() shouldBe null
        database.filterQueries.getFilterBodyOrId().executeAsOneOrNull()?.expr `should be equal to` null
        database.filterQueries.insertFilters("filter_body_json", "room_event_filter")
        database.filterQueries.get().executeAsOneOrNull() shouldNotBe null
        database.filterQueries.getFilterBodyOrId().executeAsOneOrNull()?.expr `should be equal to` "filter_body_json"
        database.filterQueries.updateFilterId("filter_id", "filter_body_json")
        database.filterQueries.getFilterBodyOrId().executeAsOneOrNull()?.expr `should be equal to` "filter_id"
    }

}
