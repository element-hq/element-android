/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.RetryTestRule
import org.matrix.android.sdk.internal.crypto.model.OlmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.util.time.DefaultClock
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmManager
import org.matrix.olm.OlmSession

private const val DUMMY_DEVICE_KEY = "DeviceKey"

@RunWith(AndroidJUnit4::class)
@Ignore
class CryptoStoreTest : InstrumentedTest {

    @get:Rule val rule = RetryTestRule(3)

    private val cryptoStoreHelper = CryptoStoreHelper()
    private val clock = DefaultClock()

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
            initOutboundSession(
                    olmAccount1,
                    olmAccount1.identityKeys()[OlmAccount.JSON_KEY_IDENTITY_KEY],
                    olmAccount1.oneTimeKeys()[OlmAccount.JSON_KEY_ONE_TIME_KEY]?.values?.first()
            )
        }

        val sessionId1 = olmSession1.sessionIdentifier()
        val olmSessionWrapper1 = OlmSessionWrapper(olmSession1)

        cryptoStore.storeSession(olmSessionWrapper1, DUMMY_DEVICE_KEY)

        assertEquals(sessionId1, cryptoStore.getLastUsedSessionId(DUMMY_DEVICE_KEY))

        val olmAccount2 = OlmAccount().apply {
            generateOneTimeKeys(1)
        }

        val olmSession2 = OlmSession().apply {
            initOutboundSession(
                    olmAccount2,
                    olmAccount2.identityKeys()[OlmAccount.JSON_KEY_IDENTITY_KEY],
                    olmAccount2.oneTimeKeys()[OlmAccount.JSON_KEY_ONE_TIME_KEY]?.values?.first()
            )
        }

        val sessionId2 = olmSession2.sessionIdentifier()
        val olmSessionWrapper2 = OlmSessionWrapper(olmSession2)

        cryptoStore.storeSession(olmSessionWrapper2, DUMMY_DEVICE_KEY)

        // Ensure sessionIds are distinct
        assertNotEquals(sessionId1, sessionId2)

        // Note: we cannot be sure what will be the result of getLastUsedSessionId() here

        olmSessionWrapper2.onMessageReceived(clock.epochMillis())
        cryptoStore.storeSession(olmSessionWrapper2, DUMMY_DEVICE_KEY)

        // sessionId2 is returned now
        assertEquals(sessionId2, cryptoStore.getLastUsedSessionId(DUMMY_DEVICE_KEY))

        Thread.sleep(2)

        olmSessionWrapper1.onMessageReceived(clock.epochMillis())
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
