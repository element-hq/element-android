/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

data class ProcessedEvent<T>(
        val type: Type,
        val event: T
) {

    enum class Type {
        KEEP,
        REMOVE
    }
}

fun <T> List<ProcessedEvent<T>>.onlyKeptEvents() = mapNotNull { processedEvent ->
    processedEvent.event.takeIf { processedEvent.type == ProcessedEvent.Type.KEEP }
}
