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

package im.vector.matrix.sqldelight.auth

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.amshove.kluent.shouldBe
import org.junit.Test
import java.util.*

class SessionParamsQueriesTest {

    @Test
    fun testAllOperations() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties())
        AuthDatabase.Schema.create(driver)
        val database = AuthDatabase(driver)

        val sessionParamsQueries: SessionParamsQueries = database.sessionParamsQueries

        sessionParamsQueries.getAllSessionParams().executeAsList().size shouldBe 0

        val sessionParams = SessionParamsEntity.Impl(
                user_id = "@userId:matrix.org",
                session_id = "sessionId",
                is_token_valid = true,
                home_server_connection_config_json = "",
                credentials_json = ""
        )
        sessionParamsQueries.insert(sessionParams)

        sessionParamsQueries.getAllSessionParams().executeAsList().size shouldBe 1


        sessionParamsQueries.getSessionParamsWithId(sessionParams.session_id).executeAsOneOrNull()?.is_token_valid shouldBe true
        sessionParamsQueries.setTokenInvalid(sessionParams.session_id)
        sessionParamsQueries.getSessionParamsWithId(sessionParams.session_id).executeAsOneOrNull()?.is_token_valid shouldBe false

        sessionParamsQueries.delete(sessionParams.session_id)
        sessionParamsQueries.getAllSessionParams().executeAsList().size shouldBe 0

    }


}
