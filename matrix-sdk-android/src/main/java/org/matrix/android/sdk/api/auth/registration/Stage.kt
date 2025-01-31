/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.auth.registration

/**
 * Registration stages.
 */
sealed class Stage(open val mandatory: Boolean) {

    /**
     * m.login.recaptcha stage.
     */
    data class ReCaptcha(override val mandatory: Boolean, val publicKey: String) : Stage(mandatory)

    /**
     * m.login.email.identity stage.
     */
    data class Email(override val mandatory: Boolean) : Stage(mandatory)

    /**
     * m.login.msisdn stage.
     */
    data class Msisdn(override val mandatory: Boolean) : Stage(mandatory)

    /**
     * m.login.dummy, can be mandatory if there is no other stages. In this case the account cannot be created by just sending a username
     * and a password, the dummy stage has to be done.
     */
    data class Dummy(override val mandatory: Boolean) : Stage(mandatory)

    /**
     * Undocumented yet: m.login.terms stage.
     */
    data class Terms(override val mandatory: Boolean, val policies: TermPolicies) : Stage(mandatory)

    /**
     * For unknown stages.
     */
    data class Other(override val mandatory: Boolean, val type: String, val params: Map<*, *>?) : Stage(mandatory)
}

typealias TermPolicies = Map<*, *>
