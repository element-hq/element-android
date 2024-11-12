/*
 * Copyright 2024 The Matrix.org Foundation C.I.C.
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

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class ComputeShieldForGroupUseCaseTest {

    @Test
    fun shouldReturnDefaultShieldWhenNoOneIsVerified() = runTest {
        val mockMachine = mockk<OlmMachine> {
            coEvery {
                getIdentity("@me:localhost")
            } returns mockk<UserIdentities>(relaxed = true)

            coEvery {
                getIdentity("@alice:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@alice:localhost")
            } returns listOf(fakeDevice("@alice:localhost", "A0", false))

            coEvery {
                getIdentity("@bob:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@bob:localhost")
            } returns listOf(fakeDevice("@bob:localhost", "B0", false))

            coEvery {
                getIdentity("@charly:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@charly:localhost")
            } returns listOf(fakeDevice("@charly:localhost", "C0", false))
        }

        val computeShieldOp = ComputeShieldForGroupUseCase("@me:localhost")

        val shield = computeShieldOp.invoke(mockMachine, listOf("@alice:localhost", "@bob:localhost", "@charly:localhost"))

        shield shouldBeEqualTo RoomEncryptionTrustLevel.Default
    }

    @Test
    fun shouldReturnDefaultShieldWhenVerifiedUsersHaveSecureDevices() = runTest {
        val mockMachine = mockk<OlmMachine> {
            coEvery {
                getIdentity("@me:localhost")
            } returns mockk<UserIdentities>(relaxed = true)

            // Alice is verified
            coEvery {
                getIdentity("@alice:localhost")
            } returns fakeIdentity(isVerified = true, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@alice:localhost")
            } returns listOf(
                    fakeDevice("@alice:localhost", "A0", true),
                    fakeDevice("@alice:localhost", "A1", true)
            )

            coEvery {
                getIdentity("@bob:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@bob:localhost")
            } returns listOf(fakeDevice("@bob:localhost", "B0", false))

            coEvery {
                getIdentity("@charly:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@charly:localhost")
            } returns listOf(fakeDevice("@charly:localhost", "C0", false))
        }

        val computeShieldOp = ComputeShieldForGroupUseCase("@me:localhost")

        val shield = computeShieldOp.invoke(mockMachine, listOf("@alice:localhost", "@bob:localhost", "@charly:localhost"))

        shield shouldBeEqualTo RoomEncryptionTrustLevel.Default
    }

    @Test
    fun shouldReturnWarningShieldWhenPreviouslyVerifiedUsersHaveInSecureDevices() = runTest {
        val mockMachine = mockk<OlmMachine> {
            coEvery {
                getIdentity("@me:localhost")
            } returns mockk<UserIdentities>(relaxed = true)

            // Alice is verified
            coEvery {
                getIdentity("@alice:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = true)

            coEvery {
                getUserDevices("@alice:localhost")
            } returns listOf(
                    fakeDevice("@alice:localhost", "A0", false),
                    fakeDevice("@alice:localhost", "A1", false)
            )

            coEvery {
                getIdentity("@bob:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@bob:localhost")
            } returns listOf(fakeDevice("@bob:localhost", "B0", false))

            coEvery {
                getIdentity("@charly:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@charly:localhost")
            } returns listOf(fakeDevice("@charly:localhost", "C0", false))
        }

        val computeShieldOp = ComputeShieldForGroupUseCase("@me:localhost")

        val shield = computeShieldOp.invoke(mockMachine, listOf("@alice:localhost", "@bob:localhost", "@charly:localhost"))

        shield shouldBeEqualTo RoomEncryptionTrustLevel.Warning
    }

    @Test
    fun shouldReturnRedShieldWhenVerifiedUserHaveInsecureDevices() = runTest {
        val mockMachine = mockk<OlmMachine> {
            coEvery {
                getIdentity("@me:localhost")
            } returns mockk<UserIdentities>(relaxed = true)

            // Alice is verified
            coEvery {
                getIdentity("@alice:localhost")
            } returns fakeIdentity(isVerified = true, hasVerificationViolation = false)

            // And has an insecure device
            coEvery {
                getUserDevices("@alice:localhost")
            } returns listOf(
                    fakeDevice("@alice:localhost", "A0", true),
                    fakeDevice("@alice:localhost", "A1", false)
            )

            coEvery {
                getIdentity("@bob:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@bob:localhost")
            } returns listOf(fakeDevice("@bob:localhost", "B0", false))

            coEvery {
                getIdentity("@charly:localhost")
            } returns fakeIdentity(isVerified = false, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@charly:localhost")
            } returns listOf(fakeDevice("@charly:localhost", "C0", false))
        }

        val computeShieldOp = ComputeShieldForGroupUseCase("@me:localhost")

        val shield = computeShieldOp.invoke(mockMachine, listOf("@alice:localhost", "@bob:localhost", "@charly:localhost"))

        shield shouldBeEqualTo RoomEncryptionTrustLevel.Warning
    }

    @Test
    fun shouldReturnGreenShieldWhenAllUsersAreVerifiedAndHaveSecuredDevices() = runTest {
        val mockMachine = mockk<OlmMachine> {
            coEvery {
                getIdentity("@me:localhost")
            } returns mockk<UserIdentities>(relaxed = true)

            // Alice is verified
            coEvery {
                getIdentity("@alice:localhost")
            } returns fakeIdentity(isVerified = true, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@alice:localhost")
            } returns listOf(
                    fakeDevice("@alice:localhost", "A0", true),
                    fakeDevice("@alice:localhost", "A1", false)
            )

            coEvery {
                getIdentity("@bob:localhost")
            } returns fakeIdentity(isVerified = true, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@bob:localhost")
            } returns listOf(fakeDevice("@bob:localhost", "B0", true))

            coEvery {
                getIdentity("@charly:localhost")
            } returns fakeIdentity(isVerified = true, hasVerificationViolation = false)

            coEvery {
                getUserDevices("@charly:localhost")
            } returns listOf(fakeDevice("@charly:localhost", "C0", true))
        }

        val computeShieldOp = ComputeShieldForGroupUseCase("@me:localhost")

        val shield = computeShieldOp.invoke(mockMachine, listOf("@alice:localhost", "@bob:localhost", "@charly:localhost"))

        shield shouldBeEqualTo RoomEncryptionTrustLevel.Warning
    }

    companion object {
        internal fun fakeDevice(userId: String, deviceId: String, isSecure: Boolean) = mockk<Device>(relaxed = true) {
            every { toCryptoDeviceInfo() } returns CryptoDeviceInfo(
                    deviceId = deviceId,
                    userId = userId,
                    trustLevel = DeviceTrustLevel(
                            crossSigningVerified = isSecure, locallyVerified = null
                    )
            )
        }

        internal fun fakeIdentity(isVerified: Boolean, hasVerificationViolation: Boolean) = mockk<UserIdentities>(relaxed = true) {
            coEvery { toMxCrossSigningInfo() } returns mockk<MXCrossSigningInfo> {
                every { wasTrustedOnce } returns hasVerificationViolation
                every { isTrusted() } returns isVerified
            }
        }
    }
}
