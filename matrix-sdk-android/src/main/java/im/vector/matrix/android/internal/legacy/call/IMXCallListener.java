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
 * This class tracks the call update.
 */
public interface IMXCallListener {

    /**
     * Called when the call state change
     *
     * @param state the new call state
     */
    void onStateDidChange(String state);

    /**
     * Called when the call fails.
     *
     * @param error the failure reason
     */
    void onCallError(String error);

    /**
     * The call view has been created.
     * It can be inserted in a custom parent view.
     *
     * @param callView the call view
     */
    void onCallViewCreated(View callView);

    /**
     * The call view has been inserted.
     * The call is ready to be started.
     * For an outgoing call, use placeCall().
     * For an incoming call, use launchIncomingCall().
     */
    void onReady();

    /**
     * The call was answered on another device.
     */
    void onCallAnsweredElsewhere();

    /**
     * Warn that the call is ended
     *
     * @param aReasonId the reason of the call ending
     */
    void onCallEnd(final int aReasonId);

    /**
     * The video preview size has been updated.
     *
     * @param width  the new width (non scaled size)
     * @param height the new height (non scaled size)
     */
    void onPreviewSizeChanged(int width, int height);
}
