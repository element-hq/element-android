/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.legacy.sync;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.sync.SyncResponse;

/**
 * Listener for the events thread that sends data back to a data handler.
 */
public class DefaultEventsThreadListener implements EventsThreadListener {

    private final MXDataHandler mDataHandler;

    public DefaultEventsThreadListener(MXDataHandler data) {
        mDataHandler = data;
    }

    @Override
    public void onSyncResponse(SyncResponse syncResponse, String fromToken, boolean isCatchingUp) {
        mDataHandler.onSyncResponse(syncResponse, fromToken, isCatchingUp);
    }

    @Override
    public void onSyncError(MatrixError matrixError) {
        mDataHandler.onSyncError(matrixError);
    }

    @Override
    public void onConfigurationError(String matrixErrorCode) {
        mDataHandler.onConfigurationError(matrixErrorCode);
    }
}
