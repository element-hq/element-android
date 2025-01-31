/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams

import com.squareup.moshi.JsonAdapter
import io.mockk.every
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeSessionParamsMapperMoshi.Companion.sessionParams
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeSessionParamsMapperMoshi.Companion.sessionParamsEntity
import org.matrix.android.sdk.test.fixtures.CredentialsFixture.aCredentials

internal class FakeCredentialsJsonAdapter {

    val instance: JsonAdapter<Credentials> = mockk()

    init {
        every { instance.fromJson(sessionParamsEntity.credentialsJson) } returns credentials
        every { instance.toJson(sessionParams.credentials) } returns CREDENTIALS_JSON
    }

    fun givenNullDeserialization() {
        every { instance.fromJson(sessionParamsEntity.credentialsJson) } returns null
    }

    fun givenNullSerialization() {
        every { instance.toJson(credentials) } returns null
    }

    companion object {
        val credentials = aCredentials()
        const val CREDENTIALS_JSON = "credentials_json"
    }
}
