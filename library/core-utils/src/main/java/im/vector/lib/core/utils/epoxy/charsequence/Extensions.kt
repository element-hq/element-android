/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.epoxy.charsequence

/**
 * Extensions to wrap CharSequence to EpoxyCharSequence.
 */
fun CharSequence.toEpoxyCharSequence() = EpoxyCharSequence(this)
