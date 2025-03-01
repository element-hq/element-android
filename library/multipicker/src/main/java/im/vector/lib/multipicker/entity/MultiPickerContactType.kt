/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.entity

data class MultiPickerContactType(
        val displayName: String,
        val photoUri: String?,
        val phoneNumberList: List<String>,
        val emailList: List<String>
)
