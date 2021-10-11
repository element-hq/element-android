/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.internal.crypto.model.OlmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmManager
import org.matrix.olm.OlmSession

private const val DUMMY_DEVICE_KEY = "DeviceKey"

@RunWith(AndroidJUnit4::class)
class CryptoStoreTest : InstrumentedTest {

    private val cryptoStoreHelper = CryptoStoreHelper()

    @Before
    fun setup() {
        Realm.init(context())
    }

//    @Test
//    fun test_metadata_realm_ok() {
//        val cryptoStore: IMXCryptoStore = cryptoStoreHelper.createStore()
//
//        assertFalse(cryptoStore.hasData())
//
//        cryptoStore.open()
//
//        assertEquals("deviceId_sample", cryptoStore.getDeviceId())
//
//        assertTrue(cryptoStore.hasData())
//
//        // Cleanup
//        cryptoStore.close()
//        cryptoStore.deleteStore()
//    }

    @Test
    fun test_lastSessionUsed() {
        // Ensure Olm is initialized
        OlmManager()

        val cryptoStore: IMXCryptoStore = cryptoStoreHelper.createStore()

        assertNull(cryptoStore.getLastUsedSessionId(DUMMY_DEVICE_KEY))

        val olmAccount1 = OlmAccount().apply {
            generateOneTimeKeys(1)
        }

        val olmSession1 = OlmSession().apply {
            initOutboundSession(olmAccount1,
                    olmAccount1.identityKeys()[OlmAccount.JSON_KEY_IDENTITY_KEY],
                    olmAccount1.oneTimeKeys()[OlmAccount.JSON_KEY_ONE_TIME_KEY]?.values?.first())
        }

        val sessionId1 = olmSession1.sessionIdentifier()
        val olmSessionWrapper1 = OlmSessionWrapper(olmSession1)

        cryptoStore.storeSession(olmSessionWrapper1, DUMMY_DEVICE_KEY)

        assertEquals(sessionId1, cryptoStore.getLastUsedSessionId(DUMMY_DEVICE_KEY))

        val olmAccount2 = OlmAccount().apply {
            generateOneTimeKeys(1)
        }

        val olmSession2 = OlmSession().apply {
            initOutboundSession(olmAccount2,
                    olmAccount2.identityKeys()[OlmAccount.JSON_KEY_IDENTITY_KEY],
                    olmAccount2.oneTimeKeys()[OlmAccount.JSON_KEY_ONE_TIME_KEY]?.values?.first())
        }

        val sessionId2 = olmSession2.sessionIdentifier()
        val olmSessionWrapper2 = OlmSessionWrapper(olmSession2)

        cryptoStore.storeSession(olmSessionWrapper2, DUMMY_DEVICE_KEY)

        // Ensure sessionIds are distinct
        assertNotEquals(sessionId1, sessionId2)

        // Note: we cannot be sure what will be the result of getLastUsedSessionId() here

        olmSessionWrapper2.onMessageReceived()
        cryptoStore.storeSession(olmSessionWrapper2, DUMMY_DEVICE_KEY)

        // sessionId2 is returned now
        assertEquals(sessionId2, cryptoStore.getLastUsedSessionId(DUMMY_DEVICE_KEY))

        Thread.sleep(2)

        olmSessionWrapper1.onMessageReceived()
        cryptoStore.storeSession(olmSessionWrapper1, DUMMY_DEVICE_KEY)

        // sessionId1 is returned now
        assertEquals(sessionId1, cryptoStore.getLastUsedSessionId(DUMMY_DEVICE_KEY))

        // Cleanup
        olmSession1.releaseSession()
        olmSession2.releaseSession()

        olmAccount1.releaseAccount()
        olmAccount2.releaseAccount()
    }
}
