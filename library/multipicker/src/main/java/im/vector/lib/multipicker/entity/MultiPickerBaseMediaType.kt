/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.entity

interface MultiPickerBaseMediaType : MultiPickerBaseType {
    val width: Int
    val height: Int
    val orientation: Int
}
