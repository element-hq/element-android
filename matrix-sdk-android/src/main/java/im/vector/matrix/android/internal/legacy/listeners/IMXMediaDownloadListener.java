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

import com.google.gson.JsonElement;

/**
 * Interface to monitor a media download.
 */
public interface IMXMediaDownloadListener {
    /**
     * provide some download stats
     */
    class DownloadStats {
        /**
         * The download id
         */
        public String mDownloadId;

        /**
         * the download progress in percentage
         */
        public int mProgress;

        /**
         * The downloaded size in bytes
         */
        public int mDownloadedSize;

        /**
         * The file size in bytes.
         */
        public int mFileSize;

        /**
         * time in seconds since the download started
         */
        public int mElapsedTime;

        /**
         * estimated remained time in seconds to download the media
         */
        public int mEstimatedRemainingTime;

        /**
         * download bit rate in KB/s
         */
        public int mBitRate;

        @Override
        public java.lang.String toString() {
            String res = "";

            res += "mProgress : " + mProgress + "%\n";
            res += "mDownloadedSize : " + mDownloadedSize + " bytes\n";
            res += "mFileSize : " + mFileSize + "bytes\n";
            res += "mElapsedTime : " + mProgress + " seconds\n";
            res += "mEstimatedRemainingTime : " + mEstimatedRemainingTime + " seconds\n";
            res += "mBitRate : " + mBitRate + " KB/s\n";

            return res;
        }
    }

    /**
     * The download starts.
     *
     * @param downloadId the download Identifier
     */
    void onDownloadStart(String downloadId);

    /**
     * The download stats have been updated.
     *
     * @param downloadId the download Identifier
     * @param stats      the download stats
     */
    void onDownloadProgress(String downloadId, DownloadStats stats);

    /**
     * The download is completed.
     *
     * @param downloadId the download Identifier
     */
    void onDownloadComplete(String downloadId);

    /**
     * The download failed.
     *
     * @param downloadId  the download Identifier
     * @param jsonElement the error
     */
    void onDownloadError(String downloadId, JsonElement jsonElement);

    /**
     * The download has been cancelled.
     *
     * @param downloadId the download Identifier
     */
    void onDownloadCancel(String downloadId);
}
