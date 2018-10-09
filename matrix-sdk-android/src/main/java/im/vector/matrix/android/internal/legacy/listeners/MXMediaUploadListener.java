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
 * A no-op class implementing {@link IMXMediaUploadListener} so listeners can just implement the methods
 * that they require.
 */
public class MXMediaUploadListener implements IMXMediaUploadListener {
    @Override
    public void onUploadStart(String uploadId) {
    }

    @Override
    public void onUploadProgress(String uploadId, UploadStats uploadStats) {
    }

    @Override
    public void onUploadCancel(String uploadId) {
    }

    @Override
    public void onUploadError(String uploadId, int serverResponseCode, String serverErrorMessage) {
    }

    @Override
    public void onUploadComplete(String uploadId, String contentUri) {
    }
}
