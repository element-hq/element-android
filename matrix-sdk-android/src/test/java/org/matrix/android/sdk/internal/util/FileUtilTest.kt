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

package org.matrix.android.sdk.internal.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.matrix.android.sdk.internal.session.DefaultFileService.Companion.DEFAULT_FILENAME
import org.matrix.android.sdk.internal.util.file.safeFileName

class FileUtilTest {

    @Test
    fun `should return original filename when valid characters are used`() {
        val fileName = "validFileName.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("validFileName.txt", result)
    }

    @Test
    fun `should replace invalid characters with underscores`() {
        val fileName = "invalid/filename:with*chars?.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("invalid_filename_with_chars_.txt", result)
    }

    @Test
    fun `should allow Cyrillic characters in the filename`() {
        val fileName = "тестовыйФайл.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("тестовыйФайл.txt", result)
    }

    @Test
    fun `should allow Han characters in the filename`() {
        val fileName = "测试文件.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("测试文件.txt", result)
    }

    @Test
    fun `should return default filename when input is null`() {
        val fileName = null
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals(DEFAULT_FILENAME, result)
    }

    @Test
    fun `should add the correct extension when missing`() {
        val fileName = "myDocument"
        val mimeType = "application/pdf"
        val result = safeFileName(fileName, mimeType)
        assertEquals("myDocument.pdf", result)
    }

    @Test
    fun `should replace invalid characters and add the correct extension`() {
        val fileName = "my*docu/ment"
        val mimeType = "application/pdf"
        val result = safeFileName(fileName, mimeType)
        assertEquals("my_docu_ment.pdf", result)
    }

    @Test
    fun `should not modify the extension if it matches the mimeType`() {
        val fileName = "report.pdf"
        val mimeType = "application/pdf"
        val result = safeFileName(fileName, mimeType)
        assertEquals("report.pdf", result)
    }

    @Test
    fun `should replace spaces with underscores`() {
        val fileName = "my report.doc"
        val mimeType = "application/msword"
        val result = safeFileName(fileName, mimeType)
        assertEquals("my_report.doc", result)
    }

    @Test
    fun `should append extension if file name has none and mimeType is valid`() {
        val fileName = "newfile"
        val mimeType = "image/jpeg"
        val result = safeFileName(fileName, mimeType)
        assertEquals("newfile.jpg", result)
    }

    @Test
    fun `should keep hyphenated names intact`() {
        val fileName = "my-file-name"
        val mimeType = "application/octet-stream"
        val result = safeFileName(fileName, mimeType)
        assertEquals("my-file-name", result)
    }
}
