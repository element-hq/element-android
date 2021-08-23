/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.rageshake

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.logging.Formatter
import java.util.logging.LogRecord

class LogFormatter : Formatter() {

    override fun format(r: LogRecord): String {
        if (!mIsTimeZoneSet) {
            DATE_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
            mIsTimeZoneSet = true
        }

        val thrown = r.thrown
        if (thrown != null) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            sw.write(r.message)
            sw.write(LINE_SEPARATOR)
            thrown.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        } else {
            val b = StringBuilder()
            val date = DATE_FORMAT.format(Date(r.millis))
            b.append(date)
            b.append("Z ")
            b.append(r.message)
            b.append(LINE_SEPARATOR)
            return b.toString()
        }
    }

    companion object {
        private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

        //            private val DATE_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss*SSSZZZZ", Locale.US)

        private var mIsTimeZoneSet = false
    }
}
