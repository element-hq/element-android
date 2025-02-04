/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

inline fun <reified T> List<T>.nextOrNull(index: Int) = getOrNull(index + 1)
inline fun <reified T> List<T>.prevOrNull(index: Int) = getOrNull(index - 1)

fun <T> List<T>.containsAllItems(vararg items: T) = this.containsAll(items.toList())
