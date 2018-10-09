/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data.store;

import android.content.Context;
import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomAccountData;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.metrics.MetricsListener;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.ReceiptData;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.group.Group;
import im.vector.matrix.android.internal.legacy.rest.model.pid.ThirdPartyIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An interface for storing and retrieving Matrix objects.
 */
public interface IMXStore {
    /**
     * Save changes in the store.
     * If the store uses permanent storage like database or file, it is the optimised time
     * to commit the last changes.
     */
    void commit();

    /**
     * Open the store.
     */
    void open();

    /**
     * Close the store.
     * Any pending operation must be complete in this call.
     */
    void close();

    /**
     * Clear the store.
     * Any pending operation must be complete in this call.
     */
    void clear();

    /**
     * @return the used context
     */
    Context getContext();

    /**
     * Indicate if the MXStore implementation stores data permanently.
     * Permanent storage allows the SDK to make less requests at the startup.
     *
     * @return true if permanent.
     */
    boolean isPermanent();

    /**
     * Check if the initial load is performed.
     *
     * @return true if it is ready.
     */
    boolean isReady();

    /**
     * Check if the read receipts are ready to be used.
     *
     * @return true if they are ready.
     */
    boolean areReceiptsReady();

    /**
     * @return true if the store is corrupted.
     */
    boolean isCorrupted();

    /**
     * Warn that the store data are corrupted.
     * It might append if an update request failed.
     *
     * @param reason the corruption reason
     */
    void setCorrupted(String reason);

    /**
     * Returns to disk usage size in bytes.
     *
     * @return disk usage size
     */
    long diskUsage();

    /**
     * Returns the latest known event stream token
     *
     * @return the event stream token
     */
    String getEventStreamToken();

    /**
     * Set the event stream token.
     *
     * @param token the event stream token
     */
    void setEventStreamToken(String token);

    /**
     * Add a MXStore listener.
     *
     * @param listener the listener
     */
    void addMXStoreListener(IMXStoreListener listener);

    /**
     * remove a MXStore listener.
     *
     * @param listener the listener
     */
    void removeMXStoreListener(IMXStoreListener listener);

    /**
     * @return the display name
     */
    String displayName();

    /**
     * Update the user display name
     *
     * @param displayName the displayname
     * @param ts          the timestamp update
     * @return true if there is an update
     */
    boolean setDisplayName(String displayName, long ts);

    /**
     * @return the avatar URL
     */
    String avatarURL();

    /**
     * Update the avatar URL
     *
     * @param avatarURL the new URL
     * @param ts        the timestamp update
     * @return true if there is an update
     */
    boolean setAvatarURL(String avatarURL, long ts);

    /**
     * @return the third party identifiers list
     */
    List<ThirdPartyIdentifier> thirdPartyIdentifiers();

    /**
     * Update the third party identifiers list.
     *
     * @param identifiers the identifiers list
     */
    void setThirdPartyIdentifiers(List<ThirdPartyIdentifier> identifiers);

    /**
     * Update the ignored user ids list.
     *
     * @param users the user ids list
     */
    void setIgnoredUserIdsList(List<String> users);

    /**
     * Update the direct chat rooms list
     *
     * @param directChatRoomsDict the direct chats map
     */
    void setDirectChatRoomsDict(Map<String, List<String>> directChatRoomsDict);

    /**
     * @return the known rooms list
     */
    Collection<Room> getRooms();

    /**
     * Retrieve a room from its room id
     *
     * @param roomId the room id
     * @return the room if it exists
     */
    Room getRoom(String roomId);

    /**
     * @return the known users lists
     */
    Collection<User> getUsers();

    /**
     * Retrieves an user by its user id.
     *
     * @param userId the user id
     * @return the user
     */
    User getUser(String userId);

    /**
     * @return the ignored user ids list
     */
    List<String> getIgnoredUserIdsList();

    /**
     * @return the direct chats rooms list
     */
    Map<String, List<String>> getDirectChatRoomsDict();

    /**
     * Flush an updated user.
     *
     * @param user the user
     */
    void storeUser(User user);

    /**
     * Flush an user from a room member.
     *
     * @param roomMember the room member
     */
    void updateUserWithRoomMemberEvent(RoomMember roomMember);

    /**
     * Flush a room.
     *
     * @param room the room
     */
    void storeRoom(Room room);

    /**
     * Store a block of room events either live or from pagination.
     *
     * @param roomId            the room id
     * @param tokensChunkEvents the events to be stored.
     * @param direction         the direction; forwards for live, backwards for pagination
     */
    void storeRoomEvents(String roomId, TokensChunkEvents tokensChunkEvents, EventTimeline.Direction direction);

    /**
     * Store the back token of a room.
     *
     * @param roomId    the room id.
     * @param backToken the back token
     */
    void storeBackToken(String roomId, String backToken);

    /**
     * Store a live room event.
     *
     * @param event The event to be stored.
     */
    void storeLiveRoomEvent(Event event);

    /**
     * @param eventId the id of the event to retrieve.
     * @param roomId  the id of the room.
     * @return true if the event exists in the store.
     */
    boolean doesEventExist(String eventId, String roomId);

    /**
     * Retrieve an event from its room Id and its Event id
     *
     * @param eventId the event id
     * @param roomId  the room Id
     * @return the event (null if it is not found)
     */
    Event getEvent(String eventId, String roomId);

    /**
     * Delete an event
     *
     * @param event The event to be deleted.
     */
    void deleteEvent(Event event);

    /**
     * Remove all sent messages in a room.
     *
     * @param roomId     the id of the room.
     * @param keepUnsent set to true to do not delete the unsent message
     */
    void deleteAllRoomMessages(String roomId, boolean keepUnsent);

    /**
     * Flush the room events.
     *
     * @param roomId the id of the room.
     */
    void flushRoomEvents(String roomId);

    /**
     * Delete the room from the storage.
     * The room data and its reference will be deleted.
     *
     * @param roomId the roomId.
     */
    void deleteRoom(String roomId);

    /**
     * Delete the room data from the storage;
     * The room data are cleared but the getRoom returned object will be the same.
     *
     * @param roomId the roomId.
     */
    void deleteRoomData(String roomId);

    /**
     * Retrieve all non-state room events for this room.
     *
     * @param roomId The room ID
     * @return A collection of events. null if there is no cached event.
     */
    Collection<Event> getRoomMessages(final String roomId);

    /**
     * Retrieve all non-state room events for this room.
     *
     * @param roomId    The room ID
     * @param fromToken the token
     * @param limit     the maximum number of messages to retrieve.
     * @return A collection of events. null if there is no cached event.
     */
    TokensChunkEvents getEarlierMessages(final String roomId, final String fromToken, final int limit);

    /**
     * Get the oldest event from the given room (to prevent pagination overlap).
     *
     * @param roomId the room id
     * @return the event
     */
    Event getOldestEvent(String roomId);

    /**
     * Get the latest event from the given room (to update summary for example)
     *
     * @param roomId the room id
     * @return the event
     */
    Event getLatestEvent(String roomId);

    /**
     * Count the number of events after the provided events id
     *
     * @param roomId  the room id.
     * @param eventId the event id to find.
     * @return the events count after this event if
     */
    int eventsCountAfter(String roomId, String eventId);

    // Design note: This is part of the store interface so the concrete implementation can leverage
    //              how they are storing the data to do this in an efficient manner (e.g. SQL JOINs)
    //              compared to calling getRooms() then getRoomEvents(roomId, limit=1) for each room
    //              (which forces single SELECTs)

    /**
     * <p>Retrieve a list of all the room summaries stored.</p>
     * Typically this method will be called when generating a 'Recent Activity' list.
     *
     * @return A collection of room summaries.
     */
    Collection<RoomSummary> getSummaries();

    /**
     * Get the stored summary for the given room.
     *
     * @param roomId the room id
     * @return the summary for the room, or null in case of error
     */
    @Nullable
    RoomSummary getSummary(String roomId);

    /**
     * Flush a room summary
     *
     * @param summary the summary.
     */
    void flushSummary(RoomSummary summary);

    /**
     * Flush the room summaries
     */
    void flushSummaries();

    /**
     * Store a new summary.
     *
     * @param summary the summary
     */
    void storeSummary(RoomSummary summary);

    /**
     * Store the room liveState.
     *
     * @param roomId roomId the id of the room.
     */
    void storeLiveStateForRoom(String roomId);

    /**
     * Store a room state event.
     * The room states are built with several events.
     *
     * @param roomId the room id
     * @param event  the event
     */
    void storeRoomStateEvent(String roomId, Event event);

    /**
     * Retrieve the room state creation events
     *
     * @param roomId   the room id
     * @param callback the asynchronous callback
     */
    void getRoomStateEvents(String roomId, ApiCallback<List<Event>> callback);

    /**
     * Return the list of latest unsent events.
     * The provided events are the unsent ones since the last sent one.
     * They are ordered.
     *
     * @param roomId the room id
     * @return list of unsent events
     */
    List<Event> getLatestUnsentEvents(String roomId);

    /**
     * Return the list of undelivered events
     *
     * @param roomId the room id
     * @return list of undelivered events
     */
    List<Event> getUndeliveredEvents(String roomId);

    /**
     * Return the list of unknown device events.
     *
     * @param roomId the room id
     * @return list of unknown device events
     */
    List<Event> getUnknownDeviceEvents(String roomId);

    /**
     * Returns the receipts list for an event in a dedicated room.
     * if sort is set to YES, they are sorted from the latest to the oldest ones.
     *
     * @param roomId      The room Id.
     * @param eventId     The event Id. (null to retrieve all existing receipts)
     * @param excludeSelf exclude the oneself read receipts.
     * @param sort        to sort them from the latest to the oldest
     * @return the receipts for an event in a dedicated room.
     */
    List<ReceiptData> getEventReceipts(String roomId, String eventId, boolean excludeSelf, boolean sort);

    /**
     * Store the receipt for an user in a room.
     * The receipt validity is checked i.e the receipt is not for an already read message.
     *
     * @param receipt The event
     * @param roomId  The roomId
     * @return true if the receipt has been stored
     */
    boolean storeReceipt(ReceiptData receipt, String roomId);

    /**
     * Get the receipt for an user in a dedicated room.
     *
     * @param roomId the room id.
     * @param userId the user id.
     * @return the dedicated receipt
     */
    ReceiptData getReceipt(String roomId, String userId);

    /**
     * Provides the unread events list.
     *
     * @param roomId the room id.
     * @param types  an array of event types strings (Event.EVENT_TYPE_XXX).
     * @return the unread events list.
     */
    List<Event> unreadEvents(String roomId, List<String> types);

    /**
     * Check if an event has been read by an user.
     *
     * @param roomId  the room Id
     * @param userId  the user id
     * @param eventId the event id
     * @return true if the user has read the message.
     */
    boolean isEventRead(String roomId, String userId, String eventId);

    /**
     * Store the user data for a room.
     *
     * @param roomId      The room Id.
     * @param accountData the account data.
     */
    void storeAccountData(String roomId, RoomAccountData accountData);

    /**
     * Provides the store preload time in milliseconds.
     *
     * @return the store preload time in milliseconds.
     */
    long getPreloadTime();

    /**
     * Provides some store stats
     *
     * @return the store stats
     */
    Map<String, Long> getStats();

    /**
     * Start a runnable from the store thread
     *
     * @param runnable the runnable to call
     */
    void post(Runnable runnable);

    /**
     * Store a group
     *
     * @param group the group to store
     */
    void storeGroup(Group group);

    /**
     * Flush a group in store.
     *
     * @param group the group
     */
    void flushGroup(Group group);

    /**
     * Delete a group
     *
     * @param groupId the group id to delete
     */
    void deleteGroup(String groupId);

    /**
     * Retrieve a group from its id.
     *
     * @param groupId the group id
     * @return the group if it exists
     */
    Group getGroup(String groupId);

    /**
     * @return the stored groups
     */
    Collection<Group> getGroups();

    /**
     * Set the URL preview status
     *
     * @param value the URL preview status
     */
    void setURLPreviewEnabled(boolean value);

    /**
     * Tells if the global URL preview is enabled.
     *
     * @return true if it is enabled
     */
    boolean isURLPreviewEnabled();

    /**
     * Update the rooms list which don't have URL previews
     *
     * @param roomIds the room ids list
     */
    void setRoomsWithoutURLPreview(Set<String> roomIds);

    /**
     * Set the user widgets
     */
    void setUserWidgets(Map<String, Object> contentDict);

    /**
     * Get the user widgets
     */
    Map<String, Object> getUserWidgets();

    /**
     * @return the room ids list which don't have URL preview enabled
     */
    Set<String> getRoomsWithoutURLPreviews();

    /**
     * Add a couple Json filter / filterId
     */
    void addFilter(String jsonFilter, String filterId);

    /**
     * Get the Map of all filters configured server side (note: only by this current instance of Riot)
     */
    Map<String, String> getFilters();

    /**
     * Set the public key of the antivirus server
     */
    void setAntivirusServerPublicKey(@Nullable String key);

    /**
     * @return the public key of the antivirus server
     */
    @Nullable
    String getAntivirusServerPublicKey();

    /**
     * Update the metrics listener
     *
     * @param metricsListener the metrics listener
     */
    void setMetricsListener(MetricsListener metricsListener);
}
