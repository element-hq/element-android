/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
