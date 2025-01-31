/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.test.fakes

import io.mockk.MockKVerificationScope
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.kotlin.where

internal class FakeRealm {

    val instance = mockk<Realm>(relaxed = true)

    inline fun <reified T : RealmModel> givenWhere(): RealmQuery<T> {
        val query = mockk<RealmQuery<T>>()
        every { instance.where<T>() } returns query
        return query
    }

    inline fun <reified T : RealmModel> verifyInsertOrUpdate(crossinline verification: MockKVerificationScope.() -> T) {
        verify { instance.insertOrUpdate(verification()) }
    }
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenFindFirst(
        result: T?
): RealmQuery<T> {
    every { findFirst() } returns result
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenFindAll(
        result: List<T>
): RealmQuery<T> {
    val realmResults = mockk<RealmResults<T>>()
    result.forEachIndexed { index, t ->
        every { realmResults[index] } returns t
    }
    every { realmResults.size } returns result.size
    every { findAll() } returns realmResults
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenEqualTo(
        fieldName: String,
        value: String
): RealmQuery<T> {
    every { equalTo(fieldName, value) } returns this
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenEqualTo(
        fieldName: String,
        value: Boolean
): RealmQuery<T> {
    every { equalTo(fieldName, value) } returns this
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenNotEqualTo(
        fieldName: String,
        value: String
): RealmQuery<T> {
    every { notEqualTo(fieldName, value) } returns this
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenIsNotEmpty(
        fieldName: String
): RealmQuery<T> {
    every { isNotEmpty(fieldName) } returns this
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenIsNotNull(
        fieldName: String
): RealmQuery<T> {
    every { isNotNull(fieldName) } returns this
    return this
}

inline fun <reified T : RealmModel> RealmQuery<T>.givenLessThan(
        fieldName: String,
        value: Long
): RealmQuery<T> {
    every { lessThan(fieldName, value) } returns this
    return this
}

/**
 * Should be called on a mocked RealmObject and not on a real RealmObject so that the underlying final method is mocked.
 */
fun RealmObject.givenDelete() {
    every { deleteFromRealm() } just runs
}
