/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.Realm
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.internal.database.mapper.EventMapper
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.SessionRealmModule
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.util.Normalizer

@RunWith(AndroidJUnit4::class)
class RealmSessionStoreMigration43Test {

    @get:Rule val configurationFactory = TestRealmConfigurationFactory()

    lateinit var context: Context
    var realm: Realm? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @After
    fun tearDown() {
        realm?.close()
    }

    @Test
    fun migrationShouldBeNeeded() {
        val realmName = "session_42.realm"
        val realmConfiguration = configurationFactory.createConfiguration(
                realmName,
                "efa9ab2c77ae06b0e767ffdb1c45b12be3c77d48d94f1ac41a7cd1d637fc59ac41f869a250453074e21ce13cfe7ed535593e7d150c08ce2bad7a2ab8c7b841f0",
                SessionRealmModule(),
                43,
                null
        )
        configurationFactory.copyRealmFromAssets(context, realmName, realmName)

        try {
            realm = Realm.getInstance(realmConfiguration)
            fail("Should need a migration")
        } catch (failure: Throwable) {
            // nop
        }
    }

    //  Database key for alias `session_db_e00482619b2597069b1f192b86de7da9`: efa9ab2c77ae06b0e767ffdb1c45b12be3c77d48d94f1ac41a7cd1d637fc59ac41f869a250453074e21ce13cfe7ed535593e7d150c08ce2bad7a2ab8c7b841f0
    // $WEJ8U6Zsx3TDZx3qmHIOKh-mXe5kqL_MnPcIkStEwwI
    // $11EtAQ8RYcudJVtw7e6B5Vm4ufCqKTOWKblY2U_wrpo
    @Test
    fun testMigration43() {
        val realmName = "session_42.realm"
        val migration = RealmSessionStoreMigration(Normalizer())
        val realmConfiguration = configurationFactory.createConfiguration(
                realmName,
                "efa9ab2c77ae06b0e767ffdb1c45b12be3c77d48d94f1ac41a7cd1d637fc59ac41f869a250453074e21ce13cfe7ed535593e7d150c08ce2bad7a2ab8c7b841f0",
                SessionRealmModule(),
                43,
                migration
        )
        configurationFactory.copyRealmFromAssets(context, realmName, realmName)

        realm = Realm.getInstance(realmConfiguration)

        // assert that the edit from 42 are migrated
        val editions = EventAnnotationsSummaryEntity
                .where(realm!!, "\$WEJ8U6Zsx3TDZx3qmHIOKh-mXe5kqL_MnPcIkStEwwI")
                .findFirst()
                ?.editSummary
                ?.editions

        editions shouldNotBe null
        editions!!.size shouldBe 1
        val firstEdition = editions.first()
        firstEdition?.eventId shouldBeEqualTo "\$DvOyA8vJxwGfTaJG3OEJVcL4isShyaVDnprihy38W28"
        firstEdition?.isLocalEcho shouldBeEqualTo false

        val editEvent = EventMapper.map(firstEdition!!.event!!)
        val body = editEvent.content.toModel<MessageContent>()?.body
        body shouldBeEqualTo "* Message 2 with edit"

        // assert that the edit from 42 are migrated
        val editionsOfE2E = EventAnnotationsSummaryEntity
                .where(realm!!, "\$11EtAQ8RYcudJVtw7e6B5Vm4ufCqKTOWKblY2U_wrpo")
                .findFirst()
                ?.editSummary
                ?.editions

        editionsOfE2E shouldNotBe null
        editionsOfE2E!!.size shouldBe 1
        val firstEditionE2E = editionsOfE2E.first()
        firstEditionE2E?.eventId shouldBeEqualTo "\$HUwJOQRCJwfPv7XSKvBPcvncjM0oR3q2tGIIIdv9Zts"
        firstEditionE2E?.isLocalEcho shouldBeEqualTo false

        val editEventE2E = EventMapper.map(firstEditionE2E!!.event!!)
        val body2 = editEventE2E.getClearContent().toModel<MessageContent>()?.body
        body2 shouldBeEqualTo "* Message 2, e2e edit"
    }
}
