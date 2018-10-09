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

package im.vector.matrix.android.internal.legacy.call;

import android.view.View;

/**
 * This class is the default implementation of IMXCallListener.
 */
public class MXCallListener implements IMXCallListener {

    @Override
    public void onStateDidChange(String state) {
    }

    @Override
    public void onCallError(String error) {
    }

    @Override
    public void onCallViewCreated(View callView) {
    }

    @Override
    public void onReady() {
    }

    @Override
    public void onCallAnsweredElsewhere() {
    }

    @Override
    public void onCallEnd(final int aReasonId) {
    }

    @Override
    public void onPreviewSizeChanged(int width, int height) {
    }
}
