/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        Versions(supportedVersions = listOf("v1.3.0")).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("v1.3.1")).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("v1.5.1")).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("r0.6.0"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to true)).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("v1.2.1"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to true)).doesServerSupportThreads() shouldBe true
        Versions(supportedVersions = listOf("r0.6.0"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to false)).doesServerSupportThreads() shouldBe false
        Versions(supportedVersions = listOf("v1.4.0"), unstableFeatures = mapOf("org.matrix.msc3440.stable" to false)).doesServerSupportThreads() shouldBe false
    }
}
