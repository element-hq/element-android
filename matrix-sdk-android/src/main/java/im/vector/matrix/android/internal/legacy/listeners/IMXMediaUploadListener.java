/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.listeners;

/**
 * Interface to monitor a media upload.
 */
public interface IMXMediaUploadListener {

    /**
     * Provide some upload stats
     */
    class UploadStats {
        /**
         * The upload id
         */
        public String mUploadId;

        /**
         * the upload progress in percentage
         */
        public int mProgress;

        /**
         * The uploaded size in bytes
         */
        public int mUploadedSize;

        /**
         * The file size in bytes.
         */
        public int mFileSize;

        /**
         * time in seconds since the upload started
         */
        public int mElapsedTime;

        /**
         * estimated remained time in seconds to upload the media
         */
        public int mEstimatedRemainingTime;

        /**
         * upload bit rate in KB/s
         */
        public int mBitRate;

        @Override
        public java.lang.String toString() {
            String res = "";

            res += "mProgress : " + mProgress + "%\n";
            res += "mUploadedSize : " + mUploadedSize + " bytes\n";
            res += "mFileSize : " + mFileSize + " bytes\n";
            res += "mElapsedTime : " + mProgress + " seconds\n";
            res += "mEstimatedRemainingTime : " + mEstimatedRemainingTime + " seconds\n";
            res += "mBitRate : " + mBitRate + " KB/s\n";

            return res;
        }
    }

    /**
     * The upload starts.
     *
     * @param uploadId the upload Identifier
     */
    void onUploadStart(String uploadId);

    /**
     * The media upload is in progress.
     *
     * @param uploadId    the upload Identifier
     * @param uploadStats the upload stats
     */
    void onUploadProgress(String uploadId, UploadStats uploadStats);

    /**
     * The upload has been cancelled.
     *
     * @param uploadId the upload Identifier
     */
    void onUploadCancel(String uploadId);

    /**
     * The upload fails.
     *
     * @param uploadId           the upload identifier
     * @param serverResponseCode the server response code
     * @param serverErrorMessage the server error message.
     */
    void onUploadError(String uploadId, int serverResponseCode, String serverErrorMessage);

    /**
     * The upload failed.
     *
     * @param uploadId   the upload identifier
     * @param contentUri the media URI on server.
     */
    void onUploadComplete(String uploadId, String contentUri);
}
