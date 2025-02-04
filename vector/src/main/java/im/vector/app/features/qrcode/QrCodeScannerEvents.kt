/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.qrcode

import im.vector.app.core.platform.VectorViewEvents

sealed class QrCodeScannerEvents : VectorViewEvents {
    data class CodeParsed(val result: String, val isQrCode: Boolean) : QrCodeScannerEvents()
    object ParseFailed : QrCodeScannerEvents()
    object SwitchMode : QrCodeScannerEvents()
}
