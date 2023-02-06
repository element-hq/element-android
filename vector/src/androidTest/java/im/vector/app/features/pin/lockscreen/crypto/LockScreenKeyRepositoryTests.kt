/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.pin.lockscreen.crypto

import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.util.DefaultBuildVersionSdkIntProvider
import java.security.KeyStore

class LockScreenKeyRepositoryTests {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val buildVersionSdkIntProvider = DefaultBuildVersionSdkIntProvider()

    private val keyStoreCryptoFactory: KeyStoreCrypto.Factory = mockk {
        every { provide(any(), any()) } answers {
            KeyStoreCrypto(arg(0), false, context, buildVersionSdkIntProvider, keyStore)
        }
    }

    private lateinit var lockScreenKeyRepository: LockScreenKeyRepository

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(LockScreenCryptoConstants.ANDROID_KEY_STORE).also { it.load(null) }
    }

    @Before
    fun setup() {
        lockScreenKeyRepository = spyk(LockScreenKeyRepository("base.pin_code", "base.system", keyStoreCryptoFactory))
    }

    @After
    fun tearDown() {
        clearAllMocks()
        keyStore.deleteEntry("base.pin_code")
        keyStore.deleteEntry("base.system")
    }

    @Test
    fun ensureSystemKeyCreatesSystemKeyIfNeeded() {
        lockScreenKeyRepository.ensureSystemKey()
        lockScreenKeyRepository.hasSystemKey().shouldBeTrue()
    }

    @Test
    fun encryptPinCodeCreatesPinCodeKey() {
        lockScreenKeyRepository.encryptPinCode("1234")
        lockScreenKeyRepository.hasPinCodeKey().shouldBeTrue()
    }

    @Test
    fun decryptPinCodeDecryptsEncodedPinCode() {
        val decodedPinCode = "1234"
        val pinCodeKeyCryptoMock = mockk<KeyStoreCrypto>(relaxed = true) {
            every { decryptToString(any<String>()) } returns decodedPinCode
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns pinCodeKeyCryptoMock
        lockScreenKeyRepository.decryptPinCode("SOME_VALUE") shouldBeEqualTo decodedPinCode
    }

    @Test
    fun isSystemKeyValidReturnsWhatKeyStoreCryptoHasValidKeyReplies() {
        val systemKeyCryptoMock = mockk<KeyStoreCrypto>(relaxed = true) {
            every { hasKey() } returns true
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns systemKeyCryptoMock

        every { systemKeyCryptoMock.hasValidKey() } returns false
        lockScreenKeyRepository.isSystemKeyValid().shouldBeFalse()

        every { systemKeyCryptoMock.hasValidKey() } returns true
        lockScreenKeyRepository.isSystemKeyValid().shouldBeTrue()
    }

    @Test
    fun hasSystemKeyReturnsTrueAfterSystemKeyIsCreated() {
        lockScreenKeyRepository.hasSystemKey().shouldBeFalse()

        lockScreenKeyRepository.ensureSystemKey()

        lockScreenKeyRepository.hasSystemKey().shouldBeTrue()
    }

    @Test
    fun hasPinCodeKeyReturnsTrueAfterPinCodeKeyIsCreated() {
        lockScreenKeyRepository.hasPinCodeKey().shouldBeFalse()

        lockScreenKeyRepository.encryptPinCode("1234")

        lockScreenKeyRepository.hasPinCodeKey().shouldBeTrue()
    }

    @Test
    fun deleteSystemKeyRemovesTheKeyFromKeyStore() {
        lockScreenKeyRepository.ensureSystemKey()
        lockScreenKeyRepository.hasSystemKey().shouldBeTrue()

        lockScreenKeyRepository.deleteSystemKey()

        lockScreenKeyRepository.hasSystemKey().shouldBeFalse()
    }

    @Test
    fun deletePinCodeKeyRemovesTheKeyFromKeyStore() {
        lockScreenKeyRepository.encryptPinCode("1234")
        lockScreenKeyRepository.hasPinCodeKey().shouldBeTrue()

        lockScreenKeyRepository.deletePinCodeKey()

        lockScreenKeyRepository.hasPinCodeKey().shouldBeFalse()
    }
}
