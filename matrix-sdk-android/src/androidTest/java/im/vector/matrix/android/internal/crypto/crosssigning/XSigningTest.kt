package im.vector.matrix.android.internal.crypto.crosssigning

import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.CryptoTestHelper
import im.vector.matrix.android.common.SessionTestParams
import im.vector.matrix.android.common.TestConstants
import im.vector.matrix.android.common.TestMatrixCallback
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
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
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class XSigningTest : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_InitializeAndStoreKeys() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val aliceLatch = CountDownLatch(1)
        aliceSession.cryptoService().crossSigningService()
                .initializeCrossSigning(UserPasswordAuth(
                        user = aliceSession.myUserId,
                        password = TestConstants.PASSWORD
                ), TestMatrixCallback(aliceLatch))

        mTestHelper.await(aliceLatch)

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
        val downloadLatch = CountDownLatch(1)
        aliceSession.cryptoService().downloadKeys(listOf(bobSession.myUserId), true, TestMatrixCallback(downloadLatch))
        mTestHelper.await(downloadLatch)

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

        val latch = CountDownLatch(2)

        aliceSession.cryptoService().crossSigningService().initializeCrossSigning(aliceAuthParams, TestMatrixCallback(latch))
        bobSession.cryptoService().crossSigningService().initializeCrossSigning(bobAuthParams, TestMatrixCallback(latch))

        mTestHelper.await(latch)

        // Check that alice can see bob keys
        val downloadLatch = CountDownLatch(1)
        val bobUserId = bobSession.myUserId
        aliceSession.cryptoService().downloadKeys(listOf(bobUserId), true, TestMatrixCallback(downloadLatch))
        mTestHelper.await(downloadLatch)

        val bobKeysFromAlicePOV = aliceSession.cryptoService().crossSigningService().getUserCrossSigningKeys(bobUserId)
        assertTrue("Bob keys from alice pov should not be trusted", bobKeysFromAlicePOV?.isTrusted() == false)

        val trustLatch = CountDownLatch(1)
        aliceSession.cryptoService().crossSigningService().trustUser(bobUserId, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                trustLatch.countDown()
            }

            override fun onFailure(failure: Throwable) {
                fail("Failed to trust bob")
            }
        })
        mTestHelper.await(trustLatch)

        // Now bobs logs in on a new device and verifies it
        // We will want to test that in alice POV, this new device would be trusted by cross signing

        val bobSession2 = mTestHelper.logIntoAccount(bobUserId, SessionTestParams(true))
        val bobSecondDeviceId = bobSession2.sessionParams.credentials.deviceId

        // Check that bob first session sees the new login
        val bobKeysLatch = CountDownLatch(1)
        bobSession.cryptoService().downloadKeys(listOf(bobUserId), true, object : MatrixCallback<MXUsersDevicesMap<CryptoDeviceInfo>> {
            override fun onFailure(failure: Throwable) {
                fail("Failed to get device")
            }

            override fun onSuccess(data: MXUsersDevicesMap<CryptoDeviceInfo>) {
                if (data.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId!!) == false) {
                    fail("Bob should see the new device")
                }
                bobKeysLatch.countDown()
            }
        })
        mTestHelper.await(bobKeysLatch)

        val bobSecondDevicePOVFirstDevice = bobSession.cryptoService().getDeviceInfo(bobUserId, bobSecondDeviceId)
        assertNotNull("Bob Second device should be known and persisted from first", bobSecondDevicePOVFirstDevice)

        // Manually mark it as trusted from first session
        val bobSignLatch = CountDownLatch(1)
        bobSession.cryptoService().crossSigningService().signDevice(bobSecondDeviceId!!, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                bobSignLatch.countDown()
            }

            override fun onFailure(failure: Throwable) {
                fail("Failed to trust bob ${failure.localizedMessage}")
            }
        })
        mTestHelper.await(bobSignLatch)

        // Now alice should cross trust bob's second device
        val aliceKeysLatch = CountDownLatch(1)
        aliceSession.cryptoService().downloadKeys(listOf(bobUserId), true, object : MatrixCallback<MXUsersDevicesMap<CryptoDeviceInfo>> {
            override fun onFailure(failure: Throwable) {
                fail("Failed to get device")
            }

            override fun onSuccess(data: MXUsersDevicesMap<CryptoDeviceInfo>) {
                // check that the device is seen
                if (data.getUserDeviceIds(bobUserId)?.contains(bobSecondDeviceId) == false) {
                    fail("Alice should see the new device")
                }
                aliceKeysLatch.countDown()
            }
        })
        mTestHelper.await(aliceKeysLatch)

        val result = aliceSession.cryptoService().crossSigningService().checkDeviceTrust(bobUserId, bobSecondDeviceId, null)
        assertTrue("Bob second device should be trusted from alice POV", result.isCrossSignedVerified())

        mTestHelper.signOutAndClose(aliceSession)
        mTestHelper.signOutAndClose(bobSession)
        mTestHelper.signOutAndClose(bobSession2)
    }
}
