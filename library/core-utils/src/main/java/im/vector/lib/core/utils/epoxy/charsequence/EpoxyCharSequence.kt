/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.epoxy.charsequence

/**
 * Wrapper for a CharSequence, which support mutation of the CharSequence, which can happen during rendering.
 */
class EpoxyCharSequence(val charSequence: CharSequence) {
    private val hash = charSequence.toString().hashCode()

    override fun hashCode() = hash
    override fun equals(other: Any?) = other is EpoxyCharSequence && other.hash == hash
}
