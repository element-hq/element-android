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

package im.vector.matrix.android.internal.legacy.data.cryptostore;

import android.content.Context;

import im.vector.matrix.android.api.auth.data.Credentials;
import im.vector.matrix.android.internal.legacy.crypto.IncomingRoomKeyRequest;
import im.vector.matrix.android.internal.legacy.crypto.OutgoingRoomKeyRequest;
import im.vector.matrix.android.internal.legacy.crypto.data.MXDeviceInfo;
import im.vector.matrix.android.internal.legacy.crypto.data.MXOlmInboundGroupSession2;
import org.matrix.olm.OlmAccount;
import org.matrix.olm.OlmSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * the crypto data store
 */
public interface IMXCryptoStore {
    /**
     * Init a crypto store for the passed getCredentials.
     *
     * @param context     the application context
     * @param credentials the getCredentials of the account.
     */
    void initWithCredentials(Context context, Credentials credentials);

    /**
     * @return if the corrupted is corrupted.
     */
    boolean isCorrupted();

    /**
     * Indicate if the store contains data for the passed account.
     *
     * @return true means that the user enabled the crypto in a previous session
     */
    boolean hasData();

    /**
     * Delete the crypto store for the passed getCredentials.
     */
    void deleteStore();

    /**
     * open any existing crypto store
     */
    void open();

    /**
     * Close the store
     */
    void close();

    /**
     * Store the device id.
     *
     * @param deviceId the device id
     */
    void storeDeviceId(String deviceId);

    /**
     * @return the device id
     */
    String getDeviceId();

    /**
     * Store the end to end account for the logged-in user.
     *
     * @param account the account to save
     */
    void storeAccount(OlmAccount account);

    /**
     * @return the olm account
     */
    OlmAccount getAccount();

    /**
     * Store a device for a user.
     *
     * @param userId The user's id.
     * @param device the device to store.
     */
    void storeUserDevice(String userId, MXDeviceInfo device);

    /**
     * Retrieve a device for a user.
     *
     * @param deviceId The device id.
     * @param userId   The user's id.
     * @return A map from device id to 'MXDevice' object for the device.
     */
    MXDeviceInfo getUserDevice(String deviceId, String userId);

    /**
     * Store the known devices for a user.
     *
     * @param userId  The user's id.
     * @param devices A map from device id to 'MXDevice' object for the device.
     */
    void storeUserDevices(String userId, Map<String, MXDeviceInfo> devices);

    /**
     * Retrieve the known devices for a user.
     *
     * @param userId The user's id.
     * @return The devices map if some devices are known, else null
     */
    Map<String, MXDeviceInfo> getUserDevices(String userId);

    /**
     * Store the crypto algorithm for a room.
     *
     * @param roomId    the id of the room.
     * @param algorithm the algorithm.
     */
    void storeRoomAlgorithm(String roomId, String algorithm);

    /**
     * Provides the algorithm used in a dedicated room.
     *
     * @param roomId the room id
     * @return the algorithm, null is the room is not encrypted
     */
    String getRoomAlgorithm(String roomId);

    /**
     * Store a session between the logged-in user and another device.
     *
     * @param session   the end-to-end session.
     * @param deviceKey the public key of the other device.
     */
    void storeSession(OlmSession session, String deviceKey);

    /**
     * Retrieve the end-to-end sessions between the logged-in user and another
     * device.
     *
     * @param deviceKey the public key of the other device.
     * @return A map from sessionId to Base64 end-to-end session.
     */
    Map<String, OlmSession> getDeviceSessions(String deviceKey);

    /**
     * Store an inbound group session.
     *
     * @param session the inbound group session and its context.
     */
    void storeInboundGroupSession(MXOlmInboundGroupSession2 session);

    /**
     * Retrieve an inbound group session.
     *
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return an inbound group session.
     */
    MXOlmInboundGroupSession2 getInboundGroupSession(String sessionId, String senderKey);

    /**
     * Retrieve the known inbound group sessions.
     *
     * @return an inbound group session.
     */
    List<MXOlmInboundGroupSession2> getInboundGroupSessions();

    /**
     * Remove an inbound group session
     *
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     */
    void removeInboundGroupSession(String sessionId, String senderKey);

    /**
     * Set the global override for whether the client should ever send encrypted
     * messages to unverified devices.
     * If false, it can still be overridden per-room.
     * If true, it overrides the per-room settings.
     *
     * @param block true to unilaterally blacklist all
     */
    void setGlobalBlacklistUnverifiedDevices(boolean block);

    /**
     * @return true to unilaterally blacklist all unverified devices.
     */
    boolean getGlobalBlacklistUnverifiedDevices();

    /**
     * Updates the rooms ids list in which the messages are not encrypted for the unverified devices.
     *
     * @param roomIds the room ids list
     */
    void setRoomsListBlacklistUnverifiedDevices(List<String> roomIds);

    /**
     * Provides the rooms ids list in which the messages are not encrypted for the unverified devices.
     *
     * @return the room Ids list
     */
    List<String> getRoomsListBlacklistUnverifiedDevices();

    /**
     * @return the devices statuses map
     */
    Map<String, Integer> getDeviceTrackingStatuses();

    /**
     * Save the device statuses
     *
     * @param deviceTrackingStatuses the device tracking statuses
     */
    void saveDeviceTrackingStatuses(Map<String, Integer> deviceTrackingStatuses);

    /**
     * Get the tracking status of a specified userId devices.
     *
     * @param userId       the user id
     * @param defaultValue the default avlue
     * @return the tracking status
     */
    int getDeviceTrackingStatus(String userId, int defaultValue);

    /**
     * Look for an existing outgoing room key request, and if none is found,
     *
     * @param requestBody the request body
     * @return an OutgoingRoomKeyRequest instance or null
     */
    OutgoingRoomKeyRequest getOutgoingRoomKeyRequest(Map<String, String> requestBody);

    /**
     * Look for an existing outgoing room key request, and if none is found,
     * + add a new one.
     *
     * @param request the request
     * @return either the same instance as passed in, or the existing one.
     */
    OutgoingRoomKeyRequest getOrAddOutgoingRoomKeyRequest(OutgoingRoomKeyRequest request);

    /**
     * Look for room key requests by state.
     *
     * @param states the states
     * @return an OutgoingRoomKeyRequest or null
     */
    OutgoingRoomKeyRequest getOutgoingRoomKeyRequestByState(Set<OutgoingRoomKeyRequest.RequestState> states);

    /**
     * Update an existing outgoing request.
     *
     * @param request the request
     */
    void updateOutgoingRoomKeyRequest(OutgoingRoomKeyRequest request);

    /**
     * Delete an outgoing room key request.
     *
     * @param transactionId the transaction id.
     */
    void deleteOutgoingRoomKeyRequest(String transactionId);

    /**
     * Store an incomingRoomKeyRequest instance
     *
     * @param incomingRoomKeyRequest the incoming key request
     */
    void storeIncomingRoomKeyRequest(IncomingRoomKeyRequest incomingRoomKeyRequest);

    /**
     * Delete an incomingRoomKeyRequest instance
     *
     * @param incomingRoomKeyRequest the incoming key request
     */
    void deleteIncomingRoomKeyRequest(IncomingRoomKeyRequest incomingRoomKeyRequest);

    /**
     * Search an IncomingRoomKeyRequest
     *
     * @param userId the user id
     * @param deviceId the device id
     * @param requestId the request id
     * @return an IncomingRoomKeyRequest if it exists, else null
     */
    IncomingRoomKeyRequest getIncomingRoomKeyRequest(String userId, String deviceId, String requestId);

    /**
     * @return the pending IncomingRoomKeyRequest requests
     */
    List<IncomingRoomKeyRequest> getPendingIncomingRoomKeyRequests();
}
