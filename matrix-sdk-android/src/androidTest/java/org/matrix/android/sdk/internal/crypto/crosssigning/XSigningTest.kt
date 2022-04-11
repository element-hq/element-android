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

package org.matrix.android.sdk.internal.crypto.crosssigning

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.crypto.crosssigning.isCrossSignedVerified
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
class XSigningTest : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)

    @Test
    fun test_InitializeAndStoreKeys() {
        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        testHelper.doSync<Unit> {
            aliceSession.cryptoService().crossSigningService()
                    .initializeCrossSigning(object : UserInteractiveAuthInterceptor {
                        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                            promise.resume(
                                    UserPasswordAuth(
                                            user = aliceSession.myUserId,
                                            password = TestConstants.PASSWORD,
                                            session = flowResponse.session
                                    )
                            )
                        }
                    }, it)
        }

        val myCrossSigningKeys = aliceSession.cryptoService().crossSigningService().getMyCrossSigningKeys()
        val masterPubKey = myCrossSigningKeys?.masterKey()
        assertNotNull("Master key should be stored", masterPubKey?.unpaddedBase64PublicKey)
        val selfSigningKey = myCrossSigningKeys?.selfSigningKey()
        assertNotNull("SelfSigned key should be stored", selfSigningKey?.unpaddedBase64PublicKey)
        val userKey = myCrossSigningKeys?.userKey()
        assertNotNull("User key should be stored", userKey?.unpaddedBase64PublicKey)

        assertTrue("Signing Keys should be trusted", myCrossSigningKeys?.isTrusted() == true)

        assertTrue("Signing Keys should be trusted", aliceSession.cryptoService().crossSigningService().checkUserTrust(aliceSession.myUserId).isVerified())

        testHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_CrossSigningCheckBobSeesTheKeys() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceAuthParams = UserPasswordAuth(
                user = aliceSession.myUserId,
                password = TestConstants.PASSWORD
        )
        val bobAuthParams = UserPasswordAuth(
                user = bobSession!!.myUserId,
                password = TestConstants.PASSWORD
        )

        testHelper.doSync<Unit> {
            aliceSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                    promise.resume(aliceAuthParams)
                }
            }, it)
        }
        testHelper.doSync<Unit> { bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        }, it) }

        // Check that alice can see bob keys
        testHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> { aliceSession.cryptoService().downloadKeys(listOf(bobSession.myUserId), true, it) }

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobSession.myUserId)
        assertNotNull("Alice can see bob Master key", bobKeysFromAlicePOV!!.masterKey())
        assertNull("Alice should not see bob User key", bobKeysFromAlicePOV.userKey())
        assertNotNull("Alice can see bob SelfSigned key", bobKeysFromAlicePOV.selfSigningKey())

        assertEquals("Bob keys from alice pov should match", bobKeysFromAlicePOV.masterKey()?.unpaddedBase64PublicKey, bobSession.cryptoService().crossSigningService().getMyCrossSigningKeys()?.masterKey()?.unpaddedBase64PublicKey)
        assertEquals("Bob keys from alice pov should match", bobKeysFromAlicePOV.selfSigningKey()?.unpaddedBase64PublicKey, bobSession.cryptoService().crossSigningService().getMyCrossSigningKeys()?.selfSigningKey()?.unpaddedBase64PublicKey)

        assertFalse("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV.isTrusted())

        cryptoTestData.cleanUp(testHelper)
    }

    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun test_CrossSigningTestAliceTrustBobNewDevice() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceAuthParams = UserPasswordAuth(
                user = aliceSession.myUserId,
                password = TestConstants.PASSWORD
        )
        val bobAuthParams = UserPasswordAuth(
                user = bobSession!!.myUserId,
                password = TestConstants.PASSWORD
        )

        testHelper.doSync<Unit> { aliceSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(aliceAuthParams)
            }
        }, it) }
        testHelper.doSync<Unit> { bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        }, it) }

        // Check that alice can see bob keys
        val bobUserId = bobSession.myUserId
        testHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> { aliceSession.cryptoService().downloadKeys(listOf(bobUserId), true, it) }

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobUserId)
        assertTrue("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV?.isTrusted() == false)

        testHelper.doSync<Unit> { aliceSession.cryptoService().crossSigningService().trustUser(bobUserId, it) }

        // Now bobs logs in on a new device and verifies it
        // We will want to test that in alice POV, this new device would be trusted by cross signing

        val bobSession2 = testHelper.logIntoAccount(bobUserId, SessionTestParams(true))
        val bobSecondDeviceId = bobSession2.sessionParams.deviceId!!

        // Check that bob first session sees the new login
        val data = testHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            bobSession.cryptoService().downloadKeys(listOf(bobUserId), true, it)
        }

        if (data.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
            fail("Bob should see the new device")
        }

        val bobSecondDevicePOVFirstDevice = bobSession.cryptoService().getDeviceInfo(bobUserId, bobSecondDeviceId)
        assertNotNull("Bob Second device should be known and persisted from first", bobSecondDevicePOVFirstDevice)

        // Manually mark it as trusted from first session
        testHelper.doSync<Unit> {
            bobSession.cryptoService().crossSigningService().trustDevice(bobSecondDeviceId, it)
        }

        // Now alice should cross trust bob's second device
        val data2 = testHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            aliceSession.cryptoService().downloadKeys(listOf(bobUserId), true, it)
        }

        // check that the device is seen
        if (data2.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
            fail("Alice should see the new device")
        }

        val result = aliceSession.cryptoService().crossSigningService().checkDeviceTrust(bobUserId, bobSecondDeviceId, null)
        assertTrue("Bob second device should be trusted from alice POV", result.isCrossSignedVerified())

        testHelper.signOutAndClose(aliceSession)
        testHelper.signOutAndClose(bobSession)
        testHelper.signOutAndClose(bobSession2)
    }
}
