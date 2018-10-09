/*
 * Copyright 2014 OpenMarket Ltd
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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.db.MXMediasCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    private static final String LOG_TAG = ImageUtils.class.getSimpleName();

    /**
     * Gets the bitmap rotation angle from the {@link android.media.ExifInterface}.
     *
     * @param context Application context for the content resolver.
     * @param uri     The URI to find the orientation for.  Must be local.
     * @return The orientation value, which may be {@link android.media.ExifInterface#ORIENTATION_UNDEFINED}.
     */
    public static int getRotationAngleForBitmap(Context context, Uri uri) {
        int orientation = getOrientationForBitmap(context, uri);

        int rotationAngle = 0;

        if (ExifInterface.ORIENTATION_ROTATE_90 == orientation) {
            rotationAngle = 90;
        } else if (ExifInterface.ORIENTATION_ROTATE_180 == orientation) {
            rotationAngle = 180;
        } else if (ExifInterface.ORIENTATION_ROTATE_270 == orientation) {
            rotationAngle = 270;
        }

        return rotationAngle;
    }

    /**
     * Gets the {@link ExifInterface} value for the orientation for this local bitmap Uri.
     *
     * @param context Application context for the content resolver.
     * @param uri     The URI to find the orientation for.  Must be local.
     * @return The orientation value, which may be {@link ExifInterface#ORIENTATION_UNDEFINED}.
     */
    public static int getOrientationForBitmap(Context context, Uri uri) {
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;

        if (uri == null) {
            return orientation;
        }

        if (TextUtils.equals(uri.getScheme(), "content")) {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, proj, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int idxData = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String path = cursor.getString(idxData);
                    if (TextUtils.isEmpty(path)) {
                        Log.w(LOG_TAG, "Cannot find path in media db for uri " + uri);
                        return orientation;
                    }
                    ExifInterface exif = new ExifInterface(path);
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                }
            } catch (Exception e) {
                // eg SecurityException from com.google.android.apps.photos.content.GooglePhotosImageProvider URIs
                // eg IOException from trying to parse the returned path as a file when it is an http uri.
                Log.e(LOG_TAG, "Cannot get orientation for bitmap: " + e.getMessage(), e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (TextUtils.equals(uri.getScheme(), "file")) {
            try {
                ExifInterface exif = new ExifInterface(uri.getPath());
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Cannot get EXIF for file uri " + uri + " because " + e.getMessage(), e);
            }
        }

        return orientation;
    }

    public static BitmapFactory.Options decodeBitmapDimensions(InputStream stream) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);
        if (o.outHeight == -1 || o.outWidth == -1) {
            // this doesn't look like an image...
            Log.e(LOG_TAG, "Cannot resize input stream, failed to get w/h.");
            return null;
        }
        return o;
    }

    public static int getSampleSize(int w, int h, int maxSize) {
        int highestDimensionSize = (h > w) ? h : w;
        double ratio = (highestDimensionSize > maxSize) ? (highestDimensionSize / maxSize) : 1.0;
        int sampleSize = Integer.highestOneBit((int) Math.floor(ratio));
        if (sampleSize == 0) {
            sampleSize = 1;
        }
        return sampleSize;
    }

    /**
     * Resize an image from its stream.
     *
     * @param fullImageStream the image stream
     * @param maxSize         the square side to draw the image in. -1 to ignore.
     * @param aSampleSize     the image dimension divider.
     * @param quality         the image quality (0 -> 100)
     * @return a stream of the resized imaged
     * @throws IOException file IO exception.
     */
    public static InputStream resizeImage(InputStream fullImageStream, int maxSize, int aSampleSize, int quality) throws IOException {
        /*
         * This is all a bit of a mess because android doesn't ship with sensible bitmap streaming libraries.
         *
         * General structure here is: (N = size of file, M = decompressed size)
         * - Copy inputstream to outstream (Usage: 2N)
         * - Release inputstream (Usage: N)
         * - Copy outstream to instream (Usage: 2N) --- This is done to make sure we can .reset() the stream else we would potentially
         *                                              have to re-download the file once we knew the dimensions of the image (!!!)
         * - Release outstream (Usage: N)
         * - Decode image dimensions, if the size is good, just return instream, else:
         * - Decode the full image with the new sample size (Usage: N + M)
         * - Release instream (Usage: M)
         * - Bitmap compress to JPEG output stream (Usage: N + M)
         * - Release bitmap (Usage: N)
         * - Return input stream of output stream (Usage: N)
         * Usages assume immediate GC, which is no guarantee. If it didn't, the total usage is 5N + M. In an extreme scenario
         * of a full 8 MP image roughly 1.85MB file (3264x2448), this equates to roughly 25 MB of memory. On average, it will
         * maybe not immediately release the streams but will probably in the future, so maybe 3N which is ~5.55MB - either
         * way this isn't cool.
         */

        ByteArrayOutputStream outstream = new ByteArrayOutputStream();

        // copy the bytes we just got to the byte array output stream so we can resize....
        byte[] buffer = new byte[2048];
        int l;
        while ((l = fullImageStream.read(buffer)) != -1) {
            outstream.write(buffer, 0, l);
        }

        // we're done with the input stream now so get rid of it (bearing in mind this could be several MB..)
        fullImageStream.close();

        // get the width/height of the image without decoding ALL THE THINGS (though this still makes a copy of the compressed image :/)
        ByteArrayInputStream bais = new ByteArrayInputStream(outstream.toByteArray());

        // allow it to GC..
        outstream.close();

        BitmapFactory.Options o = decodeBitmapDimensions(bais);
        if (o == null) {
            return null;
        }
        int w = o.outWidth;
        int h = o.outHeight;
        bais.reset(); // yay no need to re-read the stream (which is why we dumped to another stream)
        int sampleSize = (maxSize == -1) ? aSampleSize : getSampleSize(w, h, maxSize);

        if (sampleSize == 1) {
            // small optimisation
            return bais;
        } else {
            // yucky, we have to decompress the entire (albeit subsampled) bitmap into memory then dump it back into a stream
            o = new BitmapFactory.Options();
            o.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(bais, null, o);
            if (bitmap == null) {
                return null;
            }

            bais.close();

            // recopy it back into an input stream :/
            outstream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outstream);

            // cleanup
            bitmap.recycle();

            return new ByteArrayInputStream(outstream.toByteArray());
        }
    }

    /**
     * Apply rotation to the cached image (stored at imageURL).
     * The rotated image replaces the genuine one.
     *
     * @param context       the application
     * @param imageURL      the genuine image URL.
     * @param rotationAngle angle in degrees
     * @param mediasCache   the used media cache
     * @return the rotated bitmap
     */
    public static Bitmap rotateImage(Context context, String imageURL, int rotationAngle, MXMediasCache mediasCache) {
        Bitmap rotatedBitmap = null;

        try {
            Uri imageUri = Uri.parse(imageURL);

            // there is one
            if (0 != rotationAngle) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.outWidth = -1;
                options.outHeight = -1;

                // decode the bitmap
                Bitmap bitmap = null;
                try {
                    final String filename = imageUri.getPath();
                    FileInputStream imageStream = new FileInputStream(new File(filename));
                    bitmap = BitmapFactory.decodeStream(imageStream, null, options);
                    imageStream.close();
                } catch (OutOfMemoryError e) {
                    Log.e(LOG_TAG, "applyExifRotation BitmapFactory.decodeStream : " + e.getMessage(), e);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "applyExifRotation " + e.getMessage(), e);
                }

                android.graphics.Matrix bitmapMatrix = new android.graphics.Matrix();
                bitmapMatrix.postRotate(rotationAngle);
                Bitmap transformedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), bitmapMatrix, false);

                // Bitmap.createBitmap() can return the same bitmap, so do not recycle it if it is the case
                if (transformedBitmap != bitmap) {
                    bitmap.recycle();
                }

                if (null != mediasCache) {
                    mediasCache.saveBitmap(transformedBitmap, imageURL);
                }

                rotatedBitmap = transformedBitmap;
            }

        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "applyExifRotation " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(LOG_TAG, "applyExifRotation " + e.getMessage(), e);
        }

        return rotatedBitmap;
    }

    /**
     * Apply exif rotation to the cached image (stored at imageURL).
     * The rotated image replaces the genuine one.
     *
     * @param context     the application
     * @param imageURL    the genuine image URL.
     * @param mediasCache the used media cache
     * @return the rotated bitmap if the operation succeeds.
     */
    public static Bitmap applyExifRotation(Context context, String imageURL, MXMediasCache mediasCache) {
        Bitmap rotatedBitmap = null;

        try {
            Uri imageUri = Uri.parse(imageURL);
            // get the exif rotation angle
            final int rotationAngle = ImageUtils.getRotationAngleForBitmap(context, imageUri);

            if (0 != rotationAngle) {
                rotatedBitmap = rotateImage(context, imageURL, rotationAngle, mediasCache);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "applyExifRotation " + e.getMessage(), e);
        }

        return rotatedBitmap;
    }

    /**
     * Scale and apply exif rotation to an image defines by its stream.
     *
     * @param context       the context
     * @param stream        the image stream
     * @param mimeType      the mime type
     * @param maxSide       reduce the image to this square side.
     * @param rotationAngle the rotation angle
     * @param mediasCache   the media cache.
     * @return the media url
     */
    public static String scaleAndRotateImage(Context context, InputStream stream, String mimeType, int maxSide, int rotationAngle, MXMediasCache mediasCache) {
        String url = null;

        // sanity checks
        if ((null != context) && (null != stream) && (null != mediasCache)) {
            try {
                InputStream scaledStream = ImageUtils.resizeImage(stream, maxSide, 0, 75);
                url = mediasCache.saveMedia(scaledStream, null, mimeType);
                rotateImage(context, url, rotationAngle, mediasCache);
            } catch (Exception e) {
                Log.e(LOG_TAG, "rotateAndScale " + e.getMessage(), e);
            }
        }
        return url;
    }
}
