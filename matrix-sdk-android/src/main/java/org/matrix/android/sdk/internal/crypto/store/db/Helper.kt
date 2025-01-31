/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db

import android.util.Base64
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmObject
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Get realm, invoke the action, close realm, and return the result of the action.
 */
internal fun <T> doWithRealm(realmConfiguration: RealmConfiguration, action: (Realm) -> T): T {
    return Realm.getInstance(realmConfiguration).use { realm ->
        action.invoke(realm)
    }
}

/**
 * Get realm, do the query, copy from realm, close realm, and return the copied result.
 */
internal fun <T : RealmObject> doRealmQueryAndCopy(realmConfiguration: RealmConfiguration, action: (Realm) -> T?): T? {
    return Realm.getInstance(realmConfiguration).use { realm ->
        action.invoke(realm)?.let { realm.copyFromRealm(it) }
    }
}

/**
 * Get realm, do the list query, copy from realm, close realm, and return the copied result.
 */
internal fun <T : RealmObject> doRealmQueryAndCopyList(realmConfiguration: RealmConfiguration, action: (Realm) -> Iterable<T>): Iterable<T> {
    return Realm.getInstance(realmConfiguration).use { realm ->
        action.invoke(realm).let { realm.copyFromRealm(it) }
    }
}

/**
 * Get realm instance, invoke the action in a transaction and close realm.
 */
internal fun doRealmTransaction(realmConfiguration: RealmConfiguration, action: (Realm) -> Unit) {
    Realm.getInstance(realmConfiguration).use { realm ->
        realm.executeTransaction { action.invoke(it) }
    }
}

internal fun doRealmTransactionAsync(realmConfiguration: RealmConfiguration, action: (Realm) -> Unit) {
    Realm.getInstance(realmConfiguration).use { realm ->
        realm.executeTransactionAsync { action.invoke(it) }
    }
}

/**
 * Serialize any Serializable object, zip it and convert to Base64 String.
 */
internal fun serializeForRealm(o: Any?): String? {
    if (o == null) {
        return null
    }

    val baos = ByteArrayOutputStream()
    val gzis = GZIPOutputStream(baos)
    val out = ObjectOutputStream(gzis)
    out.use {
        it.writeObject(o)
    }
    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
}

/**
 * Do the opposite of serializeForRealm.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> deserializeFromRealm(string: String?): T? {
    if (string == null) {
        return null
    }
    val decodedB64 = Base64.decode(string.toByteArray(), Base64.DEFAULT)

    val bais = decodedB64.inputStream()
    val gzis = GZIPInputStream(bais)
    val ois = SafeObjectInputStream(gzis)
    return ois.use {
        it.readObject() as T
    }
}
