/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data.store;

/**
 * An default implementation of IMXStoreListener
 */
public class MXStoreListener implements IMXStoreListener {
    @Override
    public void postProcess(String accountId) {
    }

    @Override
    public void onStoreReady(String accountId) {
    }

    @Override
    public void onStoreCorrupted(String accountId, String description) {
    }

    @Override
    public void onStoreOOM(String accountId, String description) {
    }

    @Override
    public void onReadReceiptsLoaded(String roomId) {
    }
}
