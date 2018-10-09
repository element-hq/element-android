/*
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

package im.vector.matrix.android.internal.legacy.data.cryptostore;

import java.util.ArrayList;
import java.util.List;

public class MXFileCryptoStoreMetaData2 implements java.io.Serializable {
    // avoid creating another MXFileCryptoStoreMetaData3
    // set a serialVersionUID allows to update the class.
    private static final long serialVersionUID = 9166554107081078408L;

    // The obtained user id.
    public String mUserId;

    // the device id
    public String mDeviceId;

    //  The current version of the store.
    public int mVersion;

    // flag to tell if the device is announced
    // not anymore used
    public boolean mDeviceAnnounced;

    // flag to tell if the unverified devices are blacklisted for any room.
    public boolean mGlobalBlacklistUnverifiedDevices;

    // Room ids list in which the unverified devices are blacklisted
    public List<String> mBlacklistUnverifiedDevicesRoomIdsList;

    /**
     * Default constructor
     *
     * @param userId   the user id
     * @param deviceId the device id
     * @param version  the version
     */
    public MXFileCryptoStoreMetaData2(String userId, String deviceId, int version) {
        mUserId = new String(userId);
        mDeviceId = (null != deviceId) ? new String(deviceId) : null;
        mVersion = version;
        mDeviceAnnounced = false;
        mGlobalBlacklistUnverifiedDevices = false;
        mBlacklistUnverifiedDevicesRoomIdsList = new ArrayList<>();
    }

    /**
     * Constructor with the genuine metadata format data.
     *
     * @param metadata the genuine metadata format data.
     */
    public MXFileCryptoStoreMetaData2(MXFileCryptoStoreMetaData metadata) {
        mUserId = metadata.mUserId;
        mDeviceId = metadata.mDeviceId;
        mVersion = metadata.mVersion;
        mDeviceAnnounced = metadata.mDeviceAnnounced;
        mGlobalBlacklistUnverifiedDevices = false;
        mBlacklistUnverifiedDevicesRoomIdsList = new ArrayList<>();
    }
}
