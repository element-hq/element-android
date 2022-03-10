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

package org.matrix.android.sdk.api.auth.data

import org.amshove.kluent.shouldBe
import org.junit.Test
import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.auth.version.doesServerSupportThreads
import org.matrix.android.sdk.internal.auth.version.isSupportedBySdk

class VersionsKtTest {

    @Test
    fun isSupportedBySdkTooLow() {
        Versions(supportedVersions = listOf("r0.4.0")).isSupportedBySdk() shouldBe false
        Versions(supportedVersions = listOf("r0.4.1")).isSupportedBySdk() shouldBe false
    }

    @Test
    fun isSupportedBySdkUnstable() {
        Versions(supportedVersions = listOf("r0.4.0"), unstableFeatures = mapOf("m.lazy_load_members" to true)).isSupportedBySdk() shouldBe true
    }

    @Test
    fun isSupportedBySdkOk() {
        Versions(supportedVersions = listOf("r0.5.0")).isSupportedBySdk() shouldBe true
        Versions(supportedVersions = listOf("r0.5.1")).isSupportedBySdk() shouldBe true
    }

    // Was not working
    @Test
    fun isSupportedBySdkLater() {
        Versions(supportedVersions = listOf("r0.6.0")).isSupportedBySdk() shouldBe true
        Versions(supportedVersions = listOf("r0.6.1")).isSupportedBySdk() shouldBe true
    }

    // Cover cases of issue #1442
    @Test
    fun isSupportedBySdk1442() {
        Versions(supportedVersions = listOf("r0.5.0", "r0.6.0")).isSupportedBySdk() shouldBe true
        Versions(supportedVersions = listOf("r0.5.0", "r0.6.1")).isSupportedBySdk() shouldBe true
        Versions(supportedVersions = listOf("r0.6.0")).isSupportedBySdk() shouldBe true
        Versions(supportedVersions = listOf("v1.6.0")).isSupportedBySdk() shouldBe true
    }

    @Test
    fun doesServerSupportThreads() {
        Versions(supportedVersions = listOf("r0.6.0")).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("r0.9.1")).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("v1.2.0")).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("v1.3.0")).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("v1.3.1")).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("v1.5.1")).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("r0.6.0"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to true)).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("v1.2.1"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to true)).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("r0.6.0"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to false)).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("v1.4.0"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to false)).doesServerSupportThreads() shouldBe true
    }
}
