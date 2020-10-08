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

import org.matrix.android.sdk.internal.auth.version.Versions
import org.matrix.android.sdk.internal.auth.version.isSupportedBySdk
import org.amshove.kluent.shouldBe
import org.junit.Test

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
    }
}
