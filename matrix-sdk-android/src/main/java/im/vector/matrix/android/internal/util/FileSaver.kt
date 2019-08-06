/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.util

import androidx.annotation.WorkerThread
import okio.Okio
import java.io.File
import java.io.InputStream

/**
 * Save an input stream to a file with Okio
 */
@WorkerThread
fun writeToFile(inputStream: InputStream, outputFile: File) {
    Okio.buffer(Okio.source(inputStream)).use { input ->
        Okio.buffer(Okio.sink(outputFile)).use { output ->
            output.writeAll(input)
        }
    }
}