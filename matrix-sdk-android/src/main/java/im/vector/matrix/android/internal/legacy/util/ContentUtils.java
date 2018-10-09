/* 
 * Copyright 2014 OpenMarket Ltd
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Build;
import android.os.StatFs;
import android.system.Os;
import android.webkit.MimeTypeMap;

import im.vector.matrix.android.internal.legacy.rest.model.message.ImageInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Static content utility methods.
 */
public class ContentUtils {
    private static final String LOG_TAG = ContentUtils.class.getSimpleName();

    /**
     * Build an ImageInfo object based on the image at the given path.
     *
     * @param filePath the path to the image in storage
     * @return the image info
     */
    public static ImageInfo getImageInfoFromFile(String filePath) {
        ImageInfo imageInfo = new ImageInfo();
        try {
            Bitmap imageBitmap = BitmapFactory.decodeFile(filePath);
            imageInfo.w = imageBitmap.getWidth();
            imageInfo.h = imageBitmap.getHeight();

            File file = new File(filePath);
            imageInfo.size = file.length();

            imageInfo.mimetype = getMimeType(filePath);
        } catch (OutOfMemoryError oom) {
            Log.e(LOG_TAG, "## getImageInfoFromFile() : oom", oom);
        }

        return imageInfo;
    }

    public static String getMimeType(String filePath) {
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getMimeTypeFromExtension(filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase());
    }

    /**
     * Delete a directory with its content
     *
     * @param directory the base directory
     * @return true if the directory is deleted
     */
    public static boolean deleteDirectory(File directory) {
        // sanity check
        if (null == directory) {
            return false;
        }

        boolean succeed = true;

        if (directory.exists()) {
            File[] files = directory.listFiles();

            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        succeed &= deleteDirectory(files[i]);
                    } else {
                        succeed &= files[i].delete();
                    }
                }
            }
        }

        return succeed && directory.delete();
    }

    /**
     * Recursive method to compute a directory size
     *
     * @param context      the context
     * @param directory    the directory
     * @param logPathDepth the depth to log
     * @return the directory size
     */
    @SuppressLint("deprecation")
    public static long getDirectorySize(Context context, File directory, int logPathDepth) {
        StatFs statFs = new StatFs(directory.getAbsolutePath());
        long blockSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = statFs.getBlockSizeLong();
        } else {
            blockSize = statFs.getBlockSize();
        }

        if (blockSize < 0) {
            blockSize = 1;
        }

        return getDirectorySize(context, directory, logPathDepth, blockSize);
    }

    /**
     * Recursive method to compute a directory size
     *
     * @param context      the context
     * @param directory    the directory
     * @param logPathDepth the depth to log
     * @param blockSize    the filesystem block size
     * @return the directory size
     */
    public static long getDirectorySize(Context context, File directory, int logPathDepth, long blockSize) {
        long size = 0;

        File[] files = directory.listFiles();

        if (null != files) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (!file.isDirectory()) {
                    size += (file.length() / blockSize + 1) * blockSize;
                } else {
                    size += getDirectorySize(context, file, logPathDepth - 1);
                }
            }
        }

        if (logPathDepth > 0) {
            Log.d(LOG_TAG, "## getDirectorySize() " + directory.getPath() + " " + android.text.format.Formatter.formatFileSize(context, size));
        }

        return size;
    }

    @SuppressLint("NewApi")
    public static long getLastAccessTime(File file) {
        long lastAccessTime = file.lastModified();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                lastAccessTime = Os.lstat(file.getAbsolutePath()).st_atime;
            } else {
                Class<?> clazz = Class.forName("libcore.io.Libcore");
                Field field = clazz.getDeclaredField("os");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object os = field.get(null);

                Method method = os.getClass().getMethod("lstat", String.class);
                Object lstat = method.invoke(os, file.getAbsolutePath());

                field = lstat.getClass().getDeclaredField("st_atime");
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                lastAccessTime = field.getLong(lstat);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## getLastAccessTime() failed " + e.getMessage() + " for file " + file, e);
        }
        return lastAccessTime;
    }
}
