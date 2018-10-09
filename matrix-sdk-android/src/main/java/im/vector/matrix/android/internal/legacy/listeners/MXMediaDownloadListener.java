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
 * A no-op class implementing {@link IMXMediaDownloadListener} so listeners can just implement the methods
 * that they require.
 */
public class MXMediaDownloadListener implements IMXMediaDownloadListener {

    @Override
    public void onDownloadStart(String downloadId) {
    }

    @Override
    public void onDownloadProgress(String downloadId, DownloadStats stats) {
    }

    @Override
    public void onDownloadComplete(String downloadId) {
    }

    @Override
    public void onDownloadError(String downloadId, JsonElement jsonElement) {
    }

    @Override
    public void onDownloadCancel(String downloadId) {
    }
}
