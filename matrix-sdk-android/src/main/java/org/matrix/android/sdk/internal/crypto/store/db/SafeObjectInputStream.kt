/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db

import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass

/**
 * Package has been renamed from `im.vector.matrix.android` to `org.matrix.android.sdk`
 * so ensure deserialization of previously stored objects still works
 *
 * Ref: https://stackoverflow.com/questions/3884492/how-can-i-change-package-for-a-bunch-of-java-serializable-classes
 */
internal class SafeObjectInputStream(inputStream: InputStream) : ObjectInputStream(inputStream) {

    init {
        enableResolveObject(true)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    override fun readClassDescriptor(): ObjectStreamClass {
        val read = super.readClassDescriptor()
        if (read.name.startsWith("im.vector.matrix.android.")) {
            return ObjectStreamClass.lookup(Class.forName(read.name.replace("im.vector.matrix.android.", "org.matrix.android.sdk.")))
        }
        return read
    }
}
