package org.matrix.android.sdk.internal.crypto.crosssigning

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.internal.crypto.model.rest.UserPasswordAuth
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

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class XSigningTest : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_InitializeAndStoreKeys() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        mTestHelper.doSync<Unit> {
            aliceSession.cryptoService().crossSigningService()
                    .initializeCrossSigning(UserPasswordAuth(
                            user = aliceSession.myUserId,
                            password = TestConstants.PASSWORD
                    ), it)
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

        mTestHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_CrossSigningCheckBobSeesTheKeys() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

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

        mTestHelper.doSync<Unit> { aliceSession.cryptoService().crossSigningService().initializeCrossSigning(aliceAuthParams, it) }
        mTestHelper.doSync<Unit> { bobSession.cryptoService().crossSigningService().initializeCrossSigning(bobAuthParams, it) }

        // Check that alice can see bob keys
        mTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> { aliceSession.cryptoService().downloadKeys(listOf(bobSession.myUserId), true, it) }

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobSession.myUserId)
        assertNotNull("Alice can see bob Master key", bobKeysFromAlicePOV!!.masterKey())
        assertNull("Alice should not see bob User key", bobKeysFromAlicePOV.userKey())
        assertNotNull("Alice can see bob SelfSigned key", bobKeysFromAlicePOV.selfSigningKey())

        assertEquals("Bob keys from alice pov should match", bobKeysFromAlicePOV.masterKey()?.unpaddedBase64PublicKey, bobSession.cryptoService().crossSigningService().getMyCrossSigningKeys()?.masterKey()?.unpaddedBase64PublicKey)
        assertEquals("Bob keys from alice pov should match", bobKeysFromAlicePOV.selfSigningKey()?.unpaddedBase64PublicKey, bobSession.cryptoService().crossSigningService().getMyCrossSigningKeys()?.selfSigningKey()?.unpaddedBase64PublicKey)

        assertFalse("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV.isTrusted())

        mTestHelper.signOutAndClose(aliceSession)
        mTestHelper.signOutAndClose(bobSession)
    }

    @Test
    fun test_CrossSigningTestAliceTrustBobNewDevice() {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

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

        mTestHelper.doSync<Unit> { aliceSession.cryptoService().crossSigningService().initializeCrossSigning(aliceAuthParams, it) }
        mTestHelper.doSync<Unit> { bobSession.cryptoService().crossSigningService().initializeCrossSigning(bobAuthParams, it) }

        // Check that alice can see bob keys
        val bobUserId = bobSession.myUserId
        mTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> { aliceSession.cryptoService().downloadKeys(listOf(bobUserId), true, it) }

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobUserId)
        assertTrue("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV?.isTrusted() == false)

        mTestHelper.doSync<Unit> { aliceSession.cryptoService().crossSigningService().trustUser(bobUserId, it) }

        // Now bobs logs in on a new device and verifies it
        // We will want to test that in alice POV, this new device would be trusted by cross signing

        val bobSession2 = mTestHelper.logIntoAccount(bobUserId, SessionTestParams(true))
        val bobSecondDeviceId = bobSession2.sessionParams.deviceId!!

        // Check that bob first session sees the new login
        val data = mTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            bobSession.cryptoService().downloadKeys(listOf(bobUserId), true, it)
        }

        if (data.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
            fail("Bob should see the new device")
        }

        val bobSecondDevicePOVFirstDevice = bobSession.cryptoService().getDeviceInfo(bobUserId, bobSecondDeviceId)
        assertNotNull("Bob Second device should be known and persisted from first", bobSecondDevicePOVFirstDevice)

        // Manually mark it as trusted from first session
        mTestHelper.doSync<Unit> {
            bobSession.cryptoService().crossSigningService().trustDevice(bobSecondDeviceId, it)
        }

        // Now alice should cross trust bob's second device
        val data2 = mTestHelper.doSync<MXUsersDevicesMap<CryptoDeviceInfo>> {
            aliceSession.cryptoService().downloadKeys(listOf(bobUserId), true, it)
        }

        // check that the device is seen
        if (data2.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
            fail("Alice should see the new device")
        }

        val result = aliceSession.cryptoService().crossSigningService().checkDeviceTrust(bobUserId, bobSecondDeviceId, null)
        assertTrue("Bob second device should be trusted from alice POV", result.isCrossSignedVerified())

        mTestHelper.signOutAndClose(aliceSession)
        mTestHelper.signOutAndClose(bobSession)
        mTestHelper.signOutAndClose(bobSession2)
    }
}
