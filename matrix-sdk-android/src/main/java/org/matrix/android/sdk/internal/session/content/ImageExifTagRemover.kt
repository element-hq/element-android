/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.sanselan.Sanselan
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants
import org.matrix.android.sdk.internal.util.TemporaryFileCreator
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * This class is responsible for removing Exif tags from image files
 */

internal class ImageExifTagRemover @Inject constructor(
        private val temporaryFileCreator: TemporaryFileCreator
) {

    /**
     * Remove sensitive exif tags from a jpeg image file.
     * Scrubbing exif tags like GPS location and user comments
     * @param jpegImageFile The image file to be scrubbed
     */
    suspend fun removeSensitiveJpegExifTags(jpegImageFile: File): File = withContext(Dispatchers.IO) {
        val destinationFile = temporaryFileCreator.create()

        runCatching {
            FileOutputStream(destinationFile).use { fos ->
                val outputStream = BufferedOutputStream(fos)
                val outputSet = (Sanselan.getMetadata(jpegImageFile) as? JpegImageMetadata)?.exif?.outputSet

                outputSet?.let {
                    it.removeField(ExifTagConstants.EXIF_TAG_GPSINFO)
                    it.removeField(ExifTagConstants.EXIF_TAG_SUBJECT_LOCATION_1)
                    it.removeField(ExifTagConstants.EXIF_TAG_SUBJECT_LOCATION_2)
                    it.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE_REF)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_LONGITUDE)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_LONGITUDE_REF)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LONGITUDE)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LONGITUDE_REF)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_LATITUDE)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_LATITUDE_REF)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LATITUDE)
                    it.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LATITUDE_REF)
                    ExifRewriter().updateExifMetadataLossless(jpegImageFile, outputStream, it)
                } ?: let {
                    destinationFile.delete()
                    return@withContext jpegImageFile
                }
            }
        }
        destinationFile
    }

}
