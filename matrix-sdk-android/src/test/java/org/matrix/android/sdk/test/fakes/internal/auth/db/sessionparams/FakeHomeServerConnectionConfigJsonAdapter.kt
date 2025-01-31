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
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeSessionParamsMapperMoshi.Companion.sessionParams
import org.matrix.android.sdk.test.fakes.internal.auth.db.sessionparams.FakeSessionParamsMapperMoshi.Companion.sessionParamsEntity

internal class FakeHomeServerConnectionConfigJsonAdapter {

    val instance: JsonAdapter<HomeServerConnectionConfig> = mockk()

    init {
        every { instance.fromJson(sessionParamsEntity.homeServerConnectionConfigJson) } returns homeServerConnectionConfig
        every { instance.toJson(sessionParams.homeServerConnectionConfig) } returns HOME_SERVER_CONNECTION_CONFIG_JSON
    }

    fun givenNullDeserialization() {
        every { instance.fromJson(sessionParamsEntity.credentialsJson) } returns null
    }

    fun givenNullSerialization() {
        every { instance.toJson(homeServerConnectionConfig) } returns null
    }

    companion object {
        val homeServerConnectionConfig = HomeServerConnectionConfig.Builder().withHomeServerUri("homeserver").build()
        const val HOME_SERVER_CONNECTION_CONFIG_JSON = "home_server_connection_config_json"
    }
}
