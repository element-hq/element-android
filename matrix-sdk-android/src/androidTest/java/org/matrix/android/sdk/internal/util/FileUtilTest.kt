/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.internal.session.DefaultFileService.Companion.DEFAULT_FILENAME
import org.matrix.android.sdk.internal.util.file.safeFileName

/**
 * These tests are run on an Android device because they need to use the static
 * MimeTypeMap#getSingleton() method, which was failing in the unit test directory.
 */
@RunWith(AndroidJUnit4::class)
class FileUtilTest : InstrumentedTest {

    @Test
    fun shouldReturnOriginalFilenameWhenValidCharactersAreUsed() {
        val fileName = "validFileName.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("validFileName.txt", result)
    }

    @Test
    fun shouldReplaceInvalidCharactersWithUnderscores() {
        val fileName = "invalid/filename:with*chars?.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("invalid/filename_with_chars_.txt", result)
    }

    @Test
    fun shouldAllowCyrillicCharactersInTheFilename() {
        val fileName = "тестовыйФайл.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("тестовыйФайл.txt", result)
    }

    @Test
    fun shouldAllowHanCharactersInTheFilename() {
        val fileName = "测试文件.txt"
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("测试文件.txt", result)
    }

    @Test
    fun shouldReturnDefaultFilenameWhenInputIsNull() {
        val fileName = null
        val mimeType = "text/plain"
        val result = safeFileName(fileName, mimeType)
        assertEquals("$DEFAULT_FILENAME.txt", result)
    }

    @Test
    fun shouldAddTheCorrectExtensionWhenMissing() {
        val fileName = "myDocument"
        val mimeType = "application/pdf"
        val result = safeFileName(fileName, mimeType)
        assertEquals("myDocument.pdf", result)
    }

    @Test
    fun shouldReplaceInvalidCharactersAndAddTheCorrectExtension() {
        val fileName = "my*docu/ment"
        val mimeType = "application/pdf"
        val result = safeFileName(fileName, mimeType)
        assertEquals("my_docu/ment.pdf", result)
    }

    @Test
    fun shouldNotModifyTheExtensionIfItMatchesTheMimeType() {
        val fileName = "report.pdf"
        val mimeType = "application/pdf"
        val result = safeFileName(fileName, mimeType)
        assertEquals("report.pdf", result)
    }

    @Test
    fun shouldReplaceSpacesWithUnderscores() {
        val fileName = "my report.doc"
        val mimeType = "application/msword"
        val result = safeFileName(fileName, mimeType)
        assertEquals("my_report.doc", result)
    }

    @Test
    fun shouldAppendExtensionIfFileNameHasNoneAndMimeTypeIsValid() {
        val fileName = "newfile"
        val mimeType = "image/jpeg"
        val result = safeFileName(fileName, mimeType)
        assertEquals("newfile.jpg", result)
    }

    @Test
    fun shouldKeepHyphenatedNamesIntact() {
        val fileName = "my-file-name"
        val mimeType = "application/octet-stream"
        val result = safeFileName(fileName, mimeType)
        assertEquals("my-file-name.bin", result)
    }
}
