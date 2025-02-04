/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.pincode

import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeyRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Before
import org.junit.Test

class PinCodeHelperTests {

    private val lockScreenKeyRepository = mockk<LockScreenKeyRepository>(relaxed = true)
    private val storageEncrypted = mockk<EncryptedPinCodeStorage>(relaxed = true)
    lateinit var pinCodeHelper: PinCodeHelper

    @Before
    fun setup() {
        clearAllMocks()

        pinCodeHelper = PinCodeHelper(lockScreenKeyRepository, storageEncrypted)
    }

    @Test
    fun `isPinCodeEnabled returns true if a pin code key exists and the pin code encrypted value is persisted`() = runTest {
        every { lockScreenKeyRepository.hasPinCodeKey() } returns false
        coEvery { storageEncrypted.getPinCode() } returns null

        pinCodeHelper.isPinCodeAvailable().shouldBeFalse()

        every { lockScreenKeyRepository.hasPinCodeKey() } returns true

        pinCodeHelper.isPinCodeAvailable().shouldBeFalse()

        coEvery { storageEncrypted.getPinCode() } returns "SOME_ENCRYPTED_VALUE"

        pinCodeHelper.isPinCodeAvailable().shouldBeTrue()
    }

    @Test
    fun `createPinCode creates a pin code key, encrypts the actual pin code and stores it`() = runTest {
        every { lockScreenKeyRepository.encryptPinCode(any()) } returns "SOME_ENCRYPTED_VALUE"

        pinCodeHelper.createPinCode("1234")

        verify { lockScreenKeyRepository.encryptPinCode(any()) }
        coVerify { storageEncrypted.savePinCode(any()) }
    }

    @Test
    fun `deletePinCode removes the pin code key from the KeyStore and the pin code from the encrypted storage`() = runTest {
        pinCodeHelper.deletePinCode()

        verify { lockScreenKeyRepository.deletePinCodeKey() }
        coVerify { storageEncrypted.deletePinCode() }
    }

    @Test
    fun `verifyPinCode loads the encrypted pin code, decrypts it and compares it to the value provided`() = runTest {
        val originalPinCode = "1234"
        val encryptedPinCode = "SOME_ENCRYPTED_VALUE"
        coEvery { storageEncrypted.getPinCode() } returns encryptedPinCode
        every { lockScreenKeyRepository.decryptPinCode(encryptedPinCode) } returns originalPinCode
        pinCodeHelper.verifyPinCode(originalPinCode).shouldBeTrue()

        every { lockScreenKeyRepository.decryptPinCode(encryptedPinCode) } returns "SOME_OTHER_VALUE"
        pinCodeHelper.verifyPinCode(originalPinCode).shouldBeFalse()
    }
}
