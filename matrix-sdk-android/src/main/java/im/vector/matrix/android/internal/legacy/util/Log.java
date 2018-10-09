/*
 * Copyright 2017 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.legacy.util;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import im.vector.matrix.android.BuildConfig;

/**
 * Intended to mimic {@link android.util.Log} in terms of interface, but with a lot of extra behind the scenes stuff.
 */
public class Log {
    private static final String LOG_TAG = "Log";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int LOG_SIZE_BYTES = 50 * 1024 * 1024; // 50MB

    // relatively large rotation count because closing > opening the app rotates the log (!)
    private static final int LOG_ROTATION_COUNT = 15;

    private static final Logger sLogger = Logger.getLogger("org.matrix.androidsdk");
    private static FileHandler sFileHandler = null;
    private static File sCacheDirectory = null;
    private static String sFileName = "matrix";

    // determine if messsages with DEBUG level should be logged or not
    public static boolean sShouldLogDebug = BuildConfig.DEBUG;

    public enum EventTag {
        /**
         * A navigation event, e.g. onPause
         */
        NAVIGATION,
        /**
         * A user triggered event, e.g. onClick
         */
        USER,
        /**
         * User-visible notifications
         */
        NOTICE,
        /**
         * A background event e.g. incoming messages
         */
        BACKGROUND
    }

    /**
     * Initialises the logger. Should be called AFTER {@link Log#setLogDirectory(File)}.
     *
     * @param fileName the base file name
     */
    public static void init(String fileName) {
        try {
            if (!TextUtils.isEmpty(fileName)) {
                sFileName = fileName;
            }
            sFileHandler = new FileHandler(sCacheDirectory.getAbsolutePath() + "/" + sFileName + ".%g.txt", LOG_SIZE_BYTES, LOG_ROTATION_COUNT);
            sFileHandler.setFormatter(new LogFormatter());
            sLogger.setUseParentHandlers(false);
            sLogger.setLevel(Level.ALL);
            sLogger.addHandler(sFileHandler);
        } catch (IOException e) {
        }
    }

    /**
     * Set the directory to put log files.
     *
     * @param cacheDir The directory, usually {@link android.content.ContextWrapper#getCacheDir()}
     */
    public static void setLogDirectory(File cacheDir) {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        sCacheDirectory = cacheDir;
    }

    /**
     * Set the directory to put log files.
     *
     * @return the cache directory
     */
    public static File getLogDirectory() {
        return sCacheDirectory;
    }

    /**
     * Adds our own log files to the provided list of files.
     *
     * @param files The list of files to add to.
     * @return The same list with more files added.
     */
    public static List<File> addLogFiles(List<File> files) {
        try {
            // reported by GA
            if (null != sFileHandler) {
                sFileHandler.flush();
                String absPath = sCacheDirectory.getAbsolutePath();

                for (int i = 0; i <= LOG_ROTATION_COUNT; i++) {
                    String filepath = absPath + "/" + sFileName + "." + i + ".txt";
                    File file = new File(filepath);
                    if (file.exists()) {
                        files.add(file);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## addLogFiles() failed : " + e.getMessage(), e);
        }
        return files;
    }

    private static void logToFile(String level, String tag, String content) {
        if (null == sCacheDirectory) {
            return;
        }

        StringBuilder b = new StringBuilder();
        b.append(Thread.currentThread().getId());
        b.append(" ");
        b.append(level);
        b.append("/");
        b.append(tag);
        b.append(": ");
        b.append(content);
        sLogger.info(b.toString());
    }

    /**
     * Log an Throwable
     *
     * @param throwable the throwable to log
     */
    private static void logToFile(Throwable throwable) {
        if (null == sCacheDirectory || throwable == null) {
            return;
        }


        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));

        sLogger.info(errors.toString());
    }

    /**
     * Log events which can be automatically analysed
     *
     * @param tag     the EventTag
     * @param content Content to log
     */
    public static void event(EventTag tag, String content) {
        android.util.Log.v(tag.name(), content);
        logToFile("EVENT", tag.name(), content);
    }

    /**
     * Log connection information, such as urls hit, incoming data, current connection status.
     *
     * @param tag     Log tag
     * @param content Content to log
     */
    public static void con(String tag, String content) {
        android.util.Log.v(tag, content);
        logToFile("CON", tag, content);
    }

    public static void v(String tag, String content) {
        android.util.Log.v(tag, content);
        logToFile("V", tag, content);
    }

    public static void v(String tag, String content, Throwable throwable) {
        android.util.Log.v(tag, content, throwable);
        logToFile("V", tag, content);
        logToFile(throwable);
    }

    public static void d(String tag, String content) {
        if (sShouldLogDebug) {
            android.util.Log.d(tag, content);
            logToFile("D", tag, content);
        }
    }

    public static void d(String tag, String content, Throwable throwable) {
        if (sShouldLogDebug) {
            android.util.Log.d(tag, content, throwable);
            logToFile("D", tag, content);
            logToFile(throwable);
        }
    }

    public static void i(String tag, String content) {
        android.util.Log.i(tag, content);
        logToFile("I", tag, content);
    }

    public static void i(String tag, String content, Throwable throwable) {
        android.util.Log.i(tag, content, throwable);
        logToFile("I", tag, content);
        logToFile(throwable);
    }

    public static void w(String tag, String content) {
        android.util.Log.w(tag, content);
        logToFile("W", tag, content);
    }

    public static void w(String tag, String content, Throwable throwable) {
        android.util.Log.w(tag, content, throwable);
        logToFile("W", tag, content);
        logToFile(throwable);
    }

    public static void e(String tag, String content) {
        android.util.Log.e(tag, content);
        logToFile("E", tag, content);
    }

    public static void e(String tag, String content, Throwable throwable) {
        android.util.Log.e(tag, content, throwable);
        logToFile("E", tag, content);
        logToFile(throwable);
    }

    public static void wtf(String tag, String content) {
        android.util.Log.wtf(tag, content);
        logToFile("WTF", tag, content);
    }

    public static void wtf(String tag, Throwable throwable) {
        android.util.Log.wtf(tag, throwable);
        logToFile("WTF", tag, throwable.getMessage());
        logToFile(throwable);
    }

    public static void wtf(String tag, String content, Throwable throwable) {
        android.util.Log.wtf(tag, content, throwable);
        logToFile("WTF", tag, content);
        logToFile(throwable);
    }

    public static final class LogFormatter extends Formatter {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);
        private static boolean mIsTimeZoneSet = false;

        @Override
        public String format(LogRecord r) {
            if (!mIsTimeZoneSet) {
                DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
                mIsTimeZoneSet = true;
            }

            Throwable thrown = r.getThrown();
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                sw.write(r.getMessage());
                sw.write(LINE_SEPARATOR);
                thrown.printStackTrace(pw);
                pw.flush();
                return sw.toString();
            } else {
                StringBuilder b = new StringBuilder();
                String date = DATE_FORMAT.format(new Date(r.getMillis()));
                b.append(date);
                b.append("Z ");
                b.append(r.getMessage());
                b.append(LINE_SEPARATOR);
                return b.toString();
            }
        }
    }
}