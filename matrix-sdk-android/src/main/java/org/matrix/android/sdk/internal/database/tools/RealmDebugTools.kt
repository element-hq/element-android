/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.tools

import io.realm.Realm
import io.realm.RealmConfiguration
import org.matrix.android.sdk.BuildConfig

internal class RealmDebugTools(
        private val realmConfiguration: RealmConfiguration
) {
    /**
     * Get info about the DB.
     */
    fun getInfo(baseName: String): String {
        return buildString {
            append("\n$baseName Realm located at : ${realmConfiguration.realmDirectory}/${realmConfiguration.realmFileName}")

            if (BuildConfig.LOG_PRIVATE_DATA) {
                val key = realmConfiguration.encryptionKey.joinToString("") { byte -> "%02x".format(byte) }
                append("\n$baseName Realm encryption key : $key")
            }

            Realm.getInstance(realmConfiguration).use { realm ->
                // Check if we have data
                separator()
                separator()
                append("\n$baseName Realm is empty: ${realm.isEmpty}")
                var total = 0L
                val maxNameLength = realmConfiguration.realmObjectClasses.maxOf { it.simpleName.length }
                realmConfiguration.realmObjectClasses.forEach { modelClazz ->
                    val count = realm.where(modelClazz).count()
                    total += count
                    append("\n$baseName Realm - count ${modelClazz.simpleName.padEnd(maxNameLength)} : $count")
                }
                separator()
                append("\n$baseName Realm - total count: $total")
                separator()
                separator()
            }
        }
    }

    private fun StringBuilder.separator() = append("\n==============================================")
}
