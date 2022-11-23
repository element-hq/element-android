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

package org.matrix.android.sdk.internal.crypto.verification

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.KeyUsage
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo
import org.matrix.android.sdk.internal.crypto.MXCryptoAlgorithms

enum class StoreMode {
    Alice,
    Bob
}

internal class FakeCryptoStoreForVerification(private val mode: StoreMode) {

    val instance = mockk<VerificationTrustBackend>()

    init {
        every { instance.getMyDeviceId() } answers {
            when (mode) {
                StoreMode.Alice -> aliceDevice1Id
                StoreMode.Bob -> bobDeviceId
            }
        }

        // order matters here but can't find any info in doc about that
        every { instance.getUserDevice(any(), any()) } returns null
        every { instance.getUserDevice(aliceMxId, aliceDevice1Id) } returns aliceFirstDevice
        every { instance.getUserDevice(bobMxId, bobDeviceId) } returns aBobDevice

        every { instance.getUserDeviceList(aliceMxId) } returns listOf(aliceFirstDevice)
        every { instance.getUserDeviceList(bobMxId) } returns listOf(aBobDevice)
        coEvery { instance.locallyTrustDevice(any(), any()) } returns Unit

        coEvery { instance.getMyTrustedMasterKeyBase64() } answers {
            when (mode) {
                StoreMode.Alice -> {
                    aliceMSK
                }
                StoreMode.Bob -> {
                    bobMSK
                }
            }
        }

        coEvery { instance.getUserMasterKeyBase64(any()) } answers {
            val mxId = firstArg<String>()
            when (mxId) {
                aliceMxId -> aliceMSK
                bobMxId -> bobMSK
                else -> null
            }
        }

        coEvery { instance.getMyDeviceId() } answers {
            when (mode) {
                StoreMode.Alice -> aliceDevice1Id
                StoreMode.Bob -> bobDeviceId
            }
        }

        coEvery { instance.getMyDevice() } answers {
            when (mode) {
                StoreMode.Alice -> aliceFirstDevice
                StoreMode.Bob -> aBobDevice
            }
        }

        coEvery {
            instance.trustOwnDevice(any())
        } returns Unit

        coEvery {
            instance.trustUser(any())
        } returns Unit
    }

    companion object {

        val aliceMxId = "alice@example.com"
        val bobMxId = "bob@example.com"
        val bobDeviceId = "MKRJDSLYGA"
        val bobDeviceId2 = "RRIWTEKZEI"

        val aliceDevice1Id = "MGDAADVDMG"

        private val aliceMSK = "Ru4ni66dbQ6FZgUoHyyBtmjKecOHMvMSsSBZ2SABtt0"
        private val aliceSSK = "Rw6MiEn5do57mBWlWUvL6VDZJ7vAfGrTC58UXVyA0eo"
        private val aliceUSK = "3XpDI8J5T1Wy2NoGePkDiVhqZlVeVPHM83q9sUJuRcc"

        private val bobMSK = "/ZK6paR+wBkKcazPx2xijn/0g+m2KCRqdCUZ6agzaaE"
        private val bobSSK = "3/u3SRYywxRl2ul9OiRJK5zFeFnGXd0TrkcnVh1Bebk"
        private val bobUSK = "601KhaiAhDTyFDS87leWc8/LB+EAUjKgjJvPMWNLP08"

        private val aliceFirstDevice = CryptoDeviceInfo(
                deviceId = aliceDevice1Id,
                userId = aliceMxId,
                algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
                keys = mapOf(
                        "curve25519:$aliceDevice1Id" to "yDa6cWOZ/WGBqm/JMUfTUCdEbAIzKHhuIcdDbnPEhDU",
                        "ed25519:$aliceDevice1Id" to "XTge+TDwfm+WW10IGnaqEyLTSukPPzg3R1J1YvO1SBI",
                ),
                signatures = mapOf(
                        aliceMxId to mapOf(
                                "ed25519:$aliceDevice1Id"
                                        to "bPOAqM40+QSMgeEzUbYbPSZZccDDMUG00lCNdSXCoaS1gKKBGkSEaHO1OcibISIabjLYzmhp9mgtivz32fbABQ",
                                "ed25519:$aliceMSK"
                                        to "owzUsQ4Pvn35uEIc5FdVnXVRPzsVYBV8uJRUSqr4y8r5tp0DvrMArtJukKETgYEAivcZMT1lwNihHIN9xh06DA"
                        )
                ),
                unsigned = UnsignedDeviceInfo(deviceDisplayName = "Element Web"),
                trustLevel = DeviceTrustLevel(crossSigningVerified = true, locallyVerified = true)
        )

        private val aBobDevice = CryptoDeviceInfo(
                deviceId = bobDeviceId,
                userId = bobMxId,
                algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
                keys = mapOf(
                        "curve25519:$bobDeviceId" to "tWwg63Yfn//61Ylhir6z4QGejvo193E6MVHmURtYVn0",
                        "ed25519:$bobDeviceId" to "pS5NJ1LiVksQFX+p58NlphqMxE705laRVtUtZpYIAfs",
                ),
                signatures = mapOf(
                        bobMxId to mapOf(
                                "ed25519:$bobDeviceId" to "zAJqsmOSzkx8EWXcrynCsWtbgWZifN7A6DLyEBs+ZPPLnNuPN5Jwzc1Rg+oZWZaRPvOPcSL0cgcxRegSBU0NBA",
                        )
                ),
                unsigned = UnsignedDeviceInfo(deviceDisplayName = "Element Ios")
        )

        val aBobDevice2 = CryptoDeviceInfo(
                deviceId = bobDeviceId2,
                userId = bobMxId,
                algorithms = MXCryptoAlgorithms.supportedAlgorithms(),
                keys = mapOf(
                        "curve25519:$bobDeviceId" to "mE4WKAcyRRv7Gk1IDIVm0lZNzb8g9YL2eRQZUHmkkCI",
                        "ed25519:$bobDeviceId" to "yB/9LITHTqrvdXWDR2k6Qw/MDLUBWABlP9v2eYuqHPE",
                ),
                signatures = mapOf(
                        bobMxId to mapOf(
                                "ed25519:$bobDeviceId" to "zAJqsmOSzkx8EWXcrynCsWtbgWZifN7A6DLyEBs+ZPPLnNuPN5Jwzc1Rg+oZWZaRPvOPcSL0cgcxRegSBU0NBA",
                        )
                ),
                unsigned = UnsignedDeviceInfo(deviceDisplayName = "Element Android")
        )

        private val aliceMSKBase = CryptoCrossSigningKey(
                userId = aliceMxId,
                usages = listOf(KeyUsage.MASTER.value),
                keys = mapOf(
                        "ed25519$aliceMSK" to aliceMSK
                ),
                trustLevel = DeviceTrustLevel(true, true),
                signatures = emptyMap()
        )

        private val aliceSSKBase = CryptoCrossSigningKey(
                userId = aliceMxId,
                usages = listOf(KeyUsage.SELF_SIGNING.value),
                keys = mapOf(
                        "ed25519$aliceSSK" to aliceSSK
                ),
                trustLevel = null,
                signatures = emptyMap()
        )

        private val aliceUSKBase = CryptoCrossSigningKey(
                userId = aliceMxId,
                usages = listOf(KeyUsage.USER_SIGNING.value),
                keys = mapOf(
                        "ed25519$aliceUSK" to aliceUSK
                ),
                trustLevel = null,
                signatures = emptyMap()
        )

        val bobMSKBase = aliceMSKBase.copy(
                userId = bobMxId,
                keys = mapOf(
                        "ed25519$bobMSK" to bobMSK
                ),
        )
        val bobUSKBase = aliceMSKBase.copy(
                userId = bobMxId,
                keys = mapOf(
                        "ed25519$bobUSK" to bobUSK
                ),
        )
        val bobSSKBase = aliceMSKBase.copy(
                userId = bobMxId,
                keys = mapOf(
                        "ed25519$bobSSK" to bobSSK
                ),
        )
    }
}
