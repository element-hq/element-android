/*
 * Copyright 2015 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.legacy.crypto.algorithms.megolm;

import im.vector.matrix.android.internal.legacy.crypto.data.MXDeviceInfo;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.crypto.data.MXUsersDevicesMap;

import java.util.List;

public class MXOutboundSessionInfo {
    private static final String LOG_TAG = MXOutboundSessionInfo.class.getSimpleName();

    // When the session was created
    private final long mCreationTime;

    // The id of the session
    public final String mSessionId;

    // Number of times this session has been used
    public int mUseCount;

    // Devices with which we have shared the session key
    // userId -> {deviceId -> msgindex}
    public final MXUsersDevicesMap<Integer> mSharedWithDevices;

    // constructor
    public MXOutboundSessionInfo(String sessionId) {
        mSessionId = sessionId;
        mSharedWithDevices = new MXUsersDevicesMap<>();
        mCreationTime = System.currentTimeMillis();
        mUseCount = 0;
    }

    public boolean needsRotation(int rotationPeriodMsgs, int rotationPeriodMs) {
        boolean needsRotation = false;
        long sessionLifetime = System.currentTimeMillis() - mCreationTime;

        if ((mUseCount >= rotationPeriodMsgs) || (sessionLifetime >= rotationPeriodMs)) {
            Log.d(LOG_TAG, "## needsRotation() : Rotating megolm session after " + mUseCount + ", " + sessionLifetime + "ms");
            needsRotation = true;
        }

        return needsRotation;
    }

    /**
     * Determine if this session has been shared with devices which it shouldn't have been.
     *
     * @param devicesInRoom the devices map
     * @return true if we have shared the session with devices which aren't in devicesInRoom.
     */
    public boolean sharedWithTooManyDevices(MXUsersDevicesMap<MXDeviceInfo> devicesInRoom) {
        List<String> userIds = mSharedWithDevices.getUserIds();

        for (String userId : userIds) {
            if (null == devicesInRoom.getUserDeviceIds(userId)) {
                Log.d(LOG_TAG, "## sharedWithTooManyDevices() : Starting new session because we shared with " + userId);
                return true;
            }

            List<String> deviceIds = mSharedWithDevices.getUserDeviceIds(userId);

            for (String deviceId : deviceIds) {
                if (null == devicesInRoom.getObject(deviceId, userId)) {
                    Log.d(LOG_TAG, "## sharedWithTooManyDevices() : Starting new session because we shared with " + userId + ":" + deviceId);
                    return true;
                }
            }
        }

        return false;
    }
}
