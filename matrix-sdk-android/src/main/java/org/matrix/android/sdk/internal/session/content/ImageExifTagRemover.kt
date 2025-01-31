/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
 * This class is responsible for removing Exif tags from image files.
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
