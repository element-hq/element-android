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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.InputStream;

/**
 * Static resource utility methods.
 */
public class ResourceUtils {
    private static final String LOG_TAG = ResourceUtils.class.getSimpleName();

    /**
     * Mime types
     **/
    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_JPG = "image/jpg";
    public static final String MIME_TYPE_IMAGE_ALL = "image/*";
    public static final String MIME_TYPE_ALL_CONTENT = "*/*";


    public static class Resource {
        public InputStream mContentStream;
        public String mMimeType;

        public Resource(InputStream contentStream, String mimeType) {
            mContentStream = contentStream;
            mMimeType = mimeType;
        }

        /**
         * Close the content stream.
         */
        public void close() {
            try {
                mMimeType = null;

                if (null != mContentStream) {
                    mContentStream.close();
                    mContentStream = null;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Resource.close failed " + e.getLocalizedMessage(), e);
            }
        }

        /**
         * Tells if the opened resource is a jpeg one.
         *
         * @return true if the opened resource is a jpeg one.
         */
        public boolean isJpegResource() {
            return MIME_TYPE_JPEG.equals(mMimeType) || MIME_TYPE_JPG.equals(mMimeType);
        }
    }

    /**
     * Get a resource stream and metadata about it given its URI returned from onActivityResult.
     *
     * @param context  the context.
     * @param uri      the URI
     * @param mimetype the mimetype
     * @return a {@link Resource} encapsulating the opened resource stream and associated metadata
     * or {@code null} if opening the resource stream failed.
     */
    public static Resource openResource(Context context, Uri uri, String mimetype) {
        try {
            // if the mime type is not provided, try to find it out
            if (TextUtils.isEmpty(mimetype)) {
                mimetype = context.getContentResolver().getType(uri);

                // try to find the mimetype from the filename
                if (null == mimetype) {
                    String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString().toLowerCase());
                    if (extension != null) {
                        mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    }
                }
            }

            return new Resource(
                    context.getContentResolver().openInputStream(uri),
                    mimetype);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to open resource input stream", e);
        }

        return null;
    }

    /**
     * Creates a thumbnail bitmap from a media Uri
     *
     * @param context        the context
     * @param mediaUri       the media Uri
     * @param maxThumbWidth  max thumbnail width
     * @param maxThumbHeight max thumbnail height
     * @return the bitmap.
     */
    public static Bitmap createThumbnailBitmap(Context context, Uri mediaUri, int maxThumbWidth, int maxThumbHeight) {
        Bitmap thumbnailBitmap = null;
        ResourceUtils.Resource resource = ResourceUtils.openResource(context, mediaUri, null);

        // check if the resource can be i
        if (null == resource) {
            return null;
        }

        try {
            // need to decompress the high res image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            resource = ResourceUtils.openResource(context, mediaUri, null);

            // get the full size bitmap
            Bitmap fullSizeBitmap = null;

            if (null != resource) {
                try {
                    fullSizeBitmap = BitmapFactory.decodeStream(resource.mContentStream, null, options);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "BitmapFactory.decodeStream fails " + e.getLocalizedMessage(), e);
                }
            }

            // succeeds to retrieve the full size bitmap
            if (null != fullSizeBitmap) {

                // the bitmap is smaller that max sizes
                if ((fullSizeBitmap.getHeight() < maxThumbHeight) && (fullSizeBitmap.getWidth() < maxThumbWidth)) {
                    thumbnailBitmap = fullSizeBitmap;
                } else {
                    double thumbnailWidth = maxThumbWidth;
                    double thumbnailHeight = maxThumbHeight;

                    double imageWidth = fullSizeBitmap.getWidth();
                    double imageHeight = fullSizeBitmap.getHeight();

                    if (imageWidth > imageHeight) {
                        thumbnailHeight = thumbnailWidth * imageHeight / imageWidth;
                    } else {
                        thumbnailWidth = thumbnailHeight * imageWidth / imageHeight;
                    }

                    try {
                        thumbnailBitmap = Bitmap.createScaledBitmap((null == fullSizeBitmap) ? thumbnailBitmap : fullSizeBitmap,
                                (int) thumbnailWidth, (int) thumbnailHeight, false);
                    } catch (OutOfMemoryError ex) {
                        Log.e(LOG_TAG, "createThumbnailBitmap " + ex.getMessage(), ex);
                    }
                }

                // reduce the memory consumption
                if (null != fullSizeBitmap) {
                    fullSizeBitmap.recycle();
                    System.gc();
                }
            }

            if (null != resource) {
                resource.mContentStream.close();
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "createThumbnailBitmap fails " + e.getLocalizedMessage());
        }

        return thumbnailBitmap;
    }
}