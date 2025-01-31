/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.pushrules

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules
 */
enum class RuleSetKey(val value: String) {
    CONTENT("content"),
    OVERRIDE("override"),
    ROOM("room"),
    SENDER("sender"),
    UNDERRIDE("underride")
}

/**
 * Ref: https://matrix.org/docs/spec/client_server/latest#get-matrix-client-r0-pushrules-scope-kind-ruleid
 */
typealias RuleKind = RuleSetKey
