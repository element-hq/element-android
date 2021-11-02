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

import kotlinx.coroutines.withContext
import org.apache.sanselan.Sanselan
import org.apache.sanselan.formats.jpeg.JpegImageMetadata
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.util.TemporaryFileCreator
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * This class is responsible for removing Exif tags from image files
 */

internal class ImageExifTagRemover @Inject constructor(
        private val temporaryFileCreator: TemporaryFileCreator,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) {

    /**
     * Remove sensitive exif tags from a jpeg image file.
     * Scrubbing exif tags like GPS location and user comments
     * @param jpegImageFile The image file to be scrubbed
     * @return the new scrubbed image file, or the original file if the operation failed
     */
    suspend fun removeSensitiveJpegExifTags(jpegImageFile: File): File = withContext(coroutineDispatchers.io) {
        val outputSet = tryOrNull("Unable to read JpegImageMetadata") {
            (Sanselan.getMetadata(jpegImageFile) as? JpegImageMetadata)?.exif?.outputSet
        } ?: return@withContext jpegImageFile

        tryOrNull("Unable to remove ExifData") {
            outputSet.removeField(ExifTagConstants.EXIF_TAG_GPSINFO)
            outputSet.removeField(ExifTagConstants.EXIF_TAG_SUBJECT_LOCATION_1)
            outputSet.removeField(ExifTagConstants.EXIF_TAG_SUBJECT_LOCATION_2)
            outputSet.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE_REF)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_LONGITUDE)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_LONGITUDE_REF)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LONGITUDE)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LONGITUDE_REF)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_LATITUDE)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_LATITUDE_REF)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LATITUDE)
            outputSet.removeField(GPSTagConstants.GPS_TAG_GPS_DEST_LATITUDE_REF)
        } ?: return@withContext jpegImageFile

        val scrubbedFile = temporaryFileCreator.create()
        return@withContext runCatching {
            FileOutputStream(scrubbedFile).use { fos ->
                val outputStream = BufferedOutputStream(fos)
                ExifRewriter().updateExifMetadataLossless(jpegImageFile, outputStream, outputSet)
            }
        }.fold(
                onSuccess = {
                    scrubbedFile
                },
                onFailure = {
                    scrubbedFile.delete()
                    jpegImageFile
                }
        )
    }
}
