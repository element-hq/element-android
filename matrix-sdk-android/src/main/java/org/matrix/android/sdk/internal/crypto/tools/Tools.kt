/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.tools

import org.matrix.olm.OlmPkDecryption
import org.matrix.olm.OlmPkEncryption
import org.matrix.olm.OlmPkSigning
import org.matrix.olm.OlmUtility

internal fun <T> withOlmEncryption(block: (OlmPkEncryption) -> T): T {
    val olmPkEncryption = OlmPkEncryption()
    try {
        return block(olmPkEncryption)
    } finally {
        olmPkEncryption.releaseEncryption()
    }
}

internal fun <T> withOlmDecryption(block: (OlmPkDecryption) -> T): T {
    val olmPkDecryption = OlmPkDecryption()
    try {
        return block(olmPkDecryption)
    } finally {
        olmPkDecryption.releaseDecryption()
    }
}

internal fun <T> withOlmSigning(block: (OlmPkSigning) -> T): T {
    val olmPkSigning = OlmPkSigning()
    try {
        return block(olmPkSigning)
    } finally {
        olmPkSigning.releaseSigning()
    }
}

internal fun <T> withOlmUtility(block: (OlmUtility) -> T): T {
    val olmUtility = OlmUtility()
    try {
        return block(olmUtility)
    } finally {
        olmUtility.releaseUtility()
    }
}
