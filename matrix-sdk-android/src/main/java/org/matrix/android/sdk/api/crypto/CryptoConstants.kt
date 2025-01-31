/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.crypto

/**
 * Matrix algorithm value for olm.
 */
const val MXCRYPTO_ALGORITHM_OLM = "m.olm.v1.curve25519-aes-sha2"

/**
 * Matrix algorithm value for megolm.
 */
const val MXCRYPTO_ALGORITHM_MEGOLM = "m.megolm.v1.aes-sha2"

/**
 * Matrix algorithm value for megolm keys backup.
 */
const val MXCRYPTO_ALGORITHM_MEGOLM_BACKUP = "m.megolm_backup.v1.curve25519-aes-sha2"

/**
 * Secured Shared Storage algorithm constant.
 */
const val SSSS_ALGORITHM_CURVE25519_AES_SHA2 = "m.secret_storage.v1.curve25519-aes-sha2"

/* Secrets are encrypted using AES-CTR-256 and MACed using HMAC-SHA-256. **/
const val SSSS_ALGORITHM_AES_HMAC_SHA2 = "m.secret_storage.v1.aes-hmac-sha2"

// TODO Refacto: use this constants everywhere
const val ed25519 = "ed25519"
const val curve25519 = "curve25519"
