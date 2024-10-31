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
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@LargeTest
class XSigningTest : InstrumentedTest {

    @Test
    fun test_InitializeAndStoreKeys() = runSessionTest(context()) { testHelper ->
        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

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
                    })

        val myCrossSigningKeys =  aliceSession.cryptoService().crossSigningService().getMyCrossSigningKeys()

        val masterPubKey = myCrossSigningKeys?.masterKey()
        assertNotNull("Master key should be stored", masterPubKey?.unpaddedBase64PublicKey)
        val selfSigningKey = myCrossSigningKeys?.selfSigningKey()
        assertNotNull("SelfSigned key should be stored", selfSigningKey?.unpaddedBase64PublicKey)
        val userKey = myCrossSigningKeys?.userKey()
        assertNotNull("User key should be stored", userKey?.unpaddedBase64PublicKey)

        assertTrue("Signing Keys should be trusted", myCrossSigningKeys?.isTrusted() == true)

        val userTrustResult = aliceSession.cryptoService().crossSigningService().checkUserTrust(aliceSession.myUserId)
        assertTrue("Signing Keys should be trusted", userTrustResult.isVerified())

        testHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_CrossSigningCheckBobSeesTheKeys() = runCryptoTest(context()) { cryptoTestHelper, _ ->
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

        aliceSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(aliceAuthParams)
            }
        })
        bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        })

        // Check that alice can see bob keys
        aliceSession.cryptoService().downloadKeysIfNeeded(listOf(bobSession.myUserId), true)

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobSession.myUserId)

        assertNotNull("Alice can see bob Master key", bobKeysFromAlicePOV!!.masterKey())
        assertNull("Alice should not see bob User key", bobKeysFromAlicePOV.userKey())
        assertNotNull("Alice can see bob SelfSigned key", bobKeysFromAlicePOV.selfSigningKey())

        val myKeys = bobSession.cryptoService().crossSigningService().getMyCrossSigningKeys()

        assertEquals("Bob keys from alice pov should match", bobKeysFromAlicePOV.masterKey()?.unpaddedBase64PublicKey, myKeys?.masterKey()?.unpaddedBase64PublicKey)
        assertEquals("Bob keys from alice pov should match", bobKeysFromAlicePOV.selfSigningKey()?.unpaddedBase64PublicKey, myKeys?.selfSigningKey()?.unpaddedBase64PublicKey)

        assertFalse("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV.isTrusted())
    }

    @Test
    fun test_CrossSigningTestAliceTrustBobNewDevice() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
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

        aliceSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(aliceAuthParams)
            }
        })
        bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        })

        // Check that alice can see bob keys
        val bobUserId = bobSession.myUserId
        aliceSession.cryptoService().downloadKeysIfNeeded(listOf(bobUserId), true)

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobUserId)

        assertTrue("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV?.isTrusted() == false)

        aliceSession.cryptoService().crossSigningService().trustUser(bobUserId)

        // Now bobs logs in on a new device and verifies it
        // We will want to test that in alice POV, this new device would be trusted by cross signing

        val bobSession2 = testHelper.logIntoAccount(bobUserId, SessionTestParams(true))
        val bobSecondDeviceId = bobSession2.sessionParams.deviceId
        // Check that bob first session sees the new login
        val data = bobSession.cryptoService().downloadKeysIfNeeded(listOf(bobUserId), true)

        if (data.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
            fail("Bob should see the new device")
        }

        val bobSecondDevicePOVFirstDevice = bobSession.cryptoService().getCryptoDeviceInfo(bobUserId, bobSecondDeviceId)
        assertNotNull("Bob Second device should be known and persisted from first", bobSecondDevicePOVFirstDevice)

        // Manually mark it as trusted from first session
        bobSession.cryptoService().crossSigningService().trustDevice(bobSecondDeviceId)

        // Now alice should cross trust bob's second device
        val data2 = aliceSession.cryptoService().downloadKeysIfNeeded(listOf(bobUserId), true)

        // check that the device is seen
        if (data2.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
            fail("Alice should see the new device")
        }

        val result = aliceSession.cryptoService().crossSigningService().checkDeviceTrust(bobUserId, bobSecondDeviceId, null)
        assertTrue("Bob second device should be trusted from alice POV", result.isCrossSignedVerified())
    }

    @Test
    fun testWarnOnCrossSigningReset() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

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

        aliceSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(aliceAuthParams)
            }
        })
        bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        })

        cryptoTestHelper.verifySASCrossSign(aliceSession, bobSession, cryptoTestData.roomId)

        testHelper.retryPeriodically {
            aliceSession.cryptoService().crossSigningService().isUserTrusted(bobSession.myUserId)
        }

        testHelper.retryPeriodically {
            aliceSession.cryptoService().crossSigningService().checkUserTrust(bobSession.myUserId).isVerified()
        }

        aliceSession.cryptoService()
        // Ensure also that bob device is trusted
        testHelper.retryPeriodically {
            val deviceInfo = aliceSession.cryptoService().getUserDevices(bobSession.myUserId).firstOrNull()
            Timber.v("#TEST device:${deviceInfo?.shortDebugString()} trust ${deviceInfo?.trustLevel}")
            deviceInfo?.trustLevel?.crossSigningVerified == true
        }

        val currentBobMSK = aliceSession.cryptoService().crossSigningService()
                .getUserCrossSigningKeys(bobSession.myUserId)!!
                .masterKey()!!.unpaddedBase64PublicKey!!

        bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        })

        testHelper.retryPeriodically {
            val newBobMsk = aliceSession.cryptoService().crossSigningService()
                    .getUserCrossSigningKeys(bobSession.myUserId)
                    ?.masterKey()?.unpaddedBase64PublicKey
            newBobMsk != null && newBobMsk != currentBobMSK
        }

        // trick to force event to sync
        bobSession.roomService().getRoom(cryptoTestData.roomId)!!.typingService().userIsTyping()

        // assert that bob is not trusted anymore from alice s
        testHelper.retryPeriodically {
            val trust = aliceSession.cryptoService().crossSigningService().checkUserTrust(bobSession.myUserId)
            !trust.isVerified()
        }

        // trick to force event to sync
        bobSession.roomService().getRoom(cryptoTestData.roomId)!!.typingService().userStopsTyping()
        bobSession.roomService().getRoom(cryptoTestData.roomId)!!.typingService().userIsTyping()

        testHelper.retryPeriodically {
            val info = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobSession.myUserId)
            info?.wasTrustedOnce == true
        }

        // trick to force event to sync
        bobSession.roomService().getRoom(cryptoTestData.roomId)!!.typingService().userStopsTyping()
        bobSession.roomService().getRoom(cryptoTestData.roomId)!!.typingService().userIsTyping()

        testHelper.retryPeriodically {
            !aliceSession.cryptoService().crossSigningService().isUserTrusted(bobSession.myUserId)
        }

        // Ensure also that bob device are not trusted
        testHelper.retryPeriodically {
            aliceSession.cryptoService().getUserDevices(bobSession.myUserId).first().trustLevel?.crossSigningVerified != true
        }
    }
}
