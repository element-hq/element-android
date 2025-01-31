/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.api.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

/**
 * This class provides the authentication data by using user and password.
 */
@JsonClass(generateAdapter = true)
data class UserPasswordAuth(

        // device device session id
        @Json(name = "session")
        override val session: String? = null,

        // registration information
        @Json(name = "type")
        val type: String? = LoginFlowTypes.PASSWORD,

        @Json(name = "user")
        val user: String? = null,

        @Json(name = "password")
        val password: String? = null
) : UIABaseAuth {

    override fun hasAuthInfo() = password != null

    override fun copyWithSession(session: String) = this.copy(session = session)

    override fun asMap(): Map<String, *> = mapOf(
            "session" to session,
            "user" to user,
            "password" to password,
            "type" to type
    )
}
