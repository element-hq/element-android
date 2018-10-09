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

import im.vector.matrix.android.internal.legacy.crypto.data.MXDeviceInfo;
import im.vector.matrix.android.internal.legacy.crypto.data.MXUsersDevicesMap;

/**
 * This class manages the calls events.
 */
public interface IMXCallsManagerListener {
    /**
     * Called when there is an incoming call within the room.
     *
     * @param call           the incoming call
     * @param unknownDevices the unknown e2e devices list
     */
    void onIncomingCall(IMXCall call, MXUsersDevicesMap<MXDeviceInfo> unknownDevices);

    /**
     * An outgoing call is started.
     *
     * @param call the outgoing call
     */
    void onOutgoingCall(IMXCall call);

    /**
     * Called when a called has been hung up
     *
     * @param call the incoming call
     */
    void onCallHangUp(IMXCall call);

    /**
     * A voip conference started in a room.
     *
     * @param roomId the room id
     */
    void onVoipConferenceStarted(String roomId);

    /**
     * A voip conference finished in a room.
     *
     * @param roomId the room id
     */
    void onVoipConferenceFinished(String roomId);
}
