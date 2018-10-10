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

package im.vector.matrix.android.internal.legacy.rest.client;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import im.vector.matrix.android.internal.auth.data.SessionParams;
import im.vector.matrix.android.internal.legacy.RestClient;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.rest.api.RoomsApi;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.RestAdapterCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.BannedUser;
import im.vector.matrix.android.internal.legacy.rest.model.ChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.CreateRoomParams;
import im.vector.matrix.android.internal.legacy.rest.model.CreateRoomResponse;
import im.vector.matrix.android.internal.legacy.rest.model.CreatedEvent;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.EventContext;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.PowerLevels;
import im.vector.matrix.android.internal.legacy.rest.model.ReportContentParams;
import im.vector.matrix.android.internal.legacy.rest.model.RoomAliasDescription;
import im.vector.matrix.android.internal.legacy.rest.model.RoomDirectoryVisibility;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.Typing;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.filter.RoomEventFilter;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomResponse;

/**
 * Class used to make requests to the rooms API.
 */
public class RoomsRestClient extends RestClient<RoomsApi> {

    public static final int DEFAULT_MESSAGES_PAGINATION_LIMIT = 30;

    // read marker field names
    private static final String READ_MARKER_FULLY_READ = "m.fully_read";
    private static final String READ_MARKER_READ = "m.read";

    /**
     * {@inheritDoc}
     */
    public RoomsRestClient(SessionParams sessionParams) {
        super(sessionParams, RoomsApi.class, RestClient.URI_API_PREFIX_PATH_R0, false);
    }

    /**
     * Send a message to room
     *
     * @param transactionId the unique transaction id (it should avoid duplicated messages)
     * @param roomId        the room id
     * @param message       the message
     * @param callback      the callback containing the created event if successful
     */
    public void sendMessage(final String transactionId, final String roomId, final Message message, final ApiCallback<CreatedEvent> callback) {
        // privacy
        // final String description = "SendMessage : roomId " + roomId + " - message " + message.body;
        final String description = "SendMessage : roomId " + roomId;

        // the messages have their dedicated method in MXSession to be resent if there is no available network
        mApi.sendMessage(transactionId, roomId, message)
                .enqueue(new RestAdapterCallback<CreatedEvent>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        sendMessage(transactionId, roomId, message, callback);
                    }
                }));
    }

    /**
     * Send an event to a room.
     *
     * @param transactionId the unique transaction id (it should avoid duplicated messages)
     * @param roomId        the room id
     * @param eventType     the type of event
     * @param content       the event content
     * @param callback      the callback containing the created event if successful
     */
    public void sendEventToRoom(final String transactionId,
                                final String roomId,
                                final String eventType,
                                final JsonObject content,
                                final ApiCallback<CreatedEvent> callback) {
        // privacy
        //final String description = "sendEvent : roomId " + roomId + " - eventType " + eventType + " content " + content;
        final String description = "sendEvent : roomId " + roomId + " - eventType " + eventType;

        // do not retry the call invite
        // it might trigger weird behaviour on flaggy networks
        if (!TextUtils.equals(eventType, Event.EVENT_TYPE_CALL_INVITE)) {
            mApi.send(transactionId, roomId, eventType, content)
                    .enqueue(new RestAdapterCallback<CreatedEvent>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                        @Override
                        public void onRetry() {
                            sendEventToRoom(transactionId, roomId, eventType, content, callback);
                        }
                    }));
        } else {
            mApi.send(transactionId, roomId, eventType, content)
                    .enqueue(new RestAdapterCallback<CreatedEvent>(description, mUnsentEventsManager, callback, null));
        }
    }

    /**
     * Get a limited amount of messages, for the given room starting from the given token.
     * The amount of message is set to {@link #DEFAULT_MESSAGES_PAGINATION_LIMIT}.
     *
     * @param roomId          the room id
     * @param fromToken       the token identifying the message to start from Required.
     * @param direction       the direction. Required.
     * @param limit           the maximum number of messages to retrieve.
     * @param roomEventFilter A RoomEventFilter to filter returned events with. Optional.
     * @param callback        the callback called with the response. Messages will be returned in reverse order.
     */
    public void getRoomMessagesFrom(final String roomId,
                                    final String fromToken,
                                    final EventTimeline.Direction direction,
                                    final int limit,
                                    @Nullable final RoomEventFilter roomEventFilter,
                                    final ApiCallback<TokensChunkEvents> callback) {
        final String description = "messagesFrom : roomId " + roomId + " fromToken " + fromToken + "with direction " + direction + " with limit " + limit;

        mApi.getRoomMessagesFrom(roomId, fromToken, (direction == EventTimeline.Direction.BACKWARDS) ? "b" : "f", limit, toJson(roomEventFilter))
                .enqueue(new RestAdapterCallback<TokensChunkEvents>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                getRoomMessagesFrom(roomId, fromToken, direction, limit, roomEventFilter, callback);
                            }
                        }));
    }

    /**
     * Invite a user to a room.
     *
     * @param roomId   the room id
     * @param userId   the user id
     * @param callback the async callback
     */
    public void inviteUserToRoom(final String roomId, final String userId, final ApiCallback<Void> callback) {
        final String description = "inviteToRoom : roomId " + roomId + " userId " + userId;

        // TODO Do not create a User for this
        User user = new User();
        user.user_id = userId;

        mApi.invite(roomId, user)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        inviteUserToRoom(roomId, userId, callback);
                    }
                }));
    }

    /**
     * Invite a user by his email address to a room.
     *
     * @param roomId   the room id
     * @param email    the email
     * @param callback the async callback
     */
    public void inviteByEmailToRoom(final String roomId, final String email, final ApiCallback<Void> callback) {
        inviteThreePidToRoom("email", email, roomId, callback);
    }

    /**
     * Invite an user from a 3Pids.
     *
     * @param medium   the medium
     * @param address  the address
     * @param roomId   the room id
     * @param callback the async callback
     */
    private void inviteThreePidToRoom(final String medium, final String address, final String roomId, final ApiCallback<Void> callback) {
        // privacy
        //final String description = "inviteThreePidToRoom : medium " + medium + " address " + address + " roomId " + roomId;
        final String description = "inviteThreePidToRoom : medium " + medium + " roomId " + roomId;

        // This request must not have the protocol part
        String identityServer = mHsConfig.getIdentityServerUri().toString();

        if (identityServer.startsWith("http://")) {
            identityServer = identityServer.substring("http://".length());
        } else if (identityServer.startsWith("https://")) {
            identityServer = identityServer.substring("https://".length());
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put("id_server", identityServer);
        parameters.put("medium", medium);
        parameters.put("address", address);

        mApi.invite(roomId, parameters)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        inviteThreePidToRoom(medium, address, roomId, callback);
                    }
                }));
    }

    /**
     * Join a room by its roomAlias or its roomId.
     *
     * @param roomIdOrAlias the room id or the room alias
     * @param callback      the async callback
     */
    public void joinRoom(final String roomIdOrAlias, final ApiCallback<RoomResponse> callback) {
        joinRoom(roomIdOrAlias, null, callback);
    }

    /**
     * Join a room by its roomAlias or its roomId with some parameters.
     *
     * @param roomIdOrAlias the room id or the room alias
     * @param params        the joining parameters.
     * @param callback      the async callback
     */
    public void joinRoom(final String roomIdOrAlias, final Map<String, Object> params, final ApiCallback<RoomResponse> callback) {
        final String description = "joinRoom : roomId " + roomIdOrAlias;

        mApi.joinRoomByAliasOrId(roomIdOrAlias, (null == params) ? new HashMap<String, Object>() : params)
                .enqueue(new RestAdapterCallback<RoomResponse>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        joinRoom(roomIdOrAlias, params, callback);
                    }
                }));
    }

    /**
     * Leave a room.
     *
     * @param roomId   the room id
     * @param callback the async callback
     */
    public void leaveRoom(final String roomId, final ApiCallback<Void> callback) {
        final String description = "leaveRoom : roomId " + roomId;

        mApi.leave(roomId, new JsonObject())
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        leaveRoom(roomId, callback);
                    }
                }));
    }

    /**
     * Forget a room.
     *
     * @param roomId   the room id
     * @param callback the async callback
     */
    public void forgetRoom(final String roomId, final ApiCallback<Void> callback) {
        final String description = "forgetRoom : roomId " + roomId;

        mApi.forget(roomId, new JsonObject())
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        forgetRoom(roomId, callback);
                    }
                }));
    }

    /**
     * Kick a user from a room.
     *
     * @param roomId   the room id
     * @param userId   the user id
     * @param callback the async callback
     */
    public void kickFromRoom(final String roomId, final String userId, final ApiCallback<Void> callback) {
        final String description = "kickFromRoom : roomId " + roomId + " userId " + userId;

        // TODO It does not look like this in the Matrix spec
        // Kicking is done by posting that the user is now in a "leave" state
        RoomMember member = new RoomMember();
        member.membership = RoomMember.MEMBERSHIP_LEAVE;

        mApi.updateRoomMember(roomId, userId, member)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        kickFromRoom(roomId, userId, callback);
                    }
                }));
    }

    /**
     * Ban a user from a room.
     *
     * @param roomId   the room id
     * @param user     the banned user object (userId and reason for ban)
     * @param callback the async callback
     */
    public void banFromRoom(final String roomId, final BannedUser user, final ApiCallback<Void> callback) {
        final String description = "banFromRoom : roomId " + roomId + " userId " + user.userId;

        mApi.ban(roomId, user)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        banFromRoom(roomId, user, callback);
                    }
                }));
    }

    /**
     * Unban an user from a room.
     *
     * @param roomId   the room id
     * @param user     the banned user (userId)
     * @param callback the async callback
     */
    public void unbanFromRoom(final String roomId, final BannedUser user, final ApiCallback<Void> callback) {
        final String description = "Unban : roomId " + roomId + " userId " + user.userId;

        mApi.unban(roomId, user)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        unbanFromRoom(roomId, user, callback);
                    }
                }));
    }

    /**
     * Create a new room.
     *
     * @param params   the room creation parameters
     * @param callback the async callback
     */
    public void createRoom(final CreateRoomParams params, final ApiCallback<CreateRoomResponse> callback) {
        // privacy
        //final String description = "createRoom : name " + name + " topic " + topic;
        final String description = "createRoom";

        mApi.createRoom(params)
                .enqueue(new RestAdapterCallback<CreateRoomResponse>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                createRoom(params, callback);
                            }
                        }));
    }

    /**
     * Perform an initial sync on the room
     *
     * @param roomId   the room id
     * @param callback the async callback
     */
    public void initialSync(final String roomId, final ApiCallback<RoomResponse> callback) {
        final String description = "initialSync : roomId " + roomId;

        mApi.initialSync(roomId, DEFAULT_MESSAGES_PAGINATION_LIMIT)
                .enqueue(new RestAdapterCallback<RoomResponse>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        initialSync(roomId, callback);
                    }
                }));
    }

    /**
     * Retrieve an event from its room id / event id.
     *
     * @param roomId   the room id
     * @param eventId  the event id
     * @param callback the asynchronous callback.
     */
    public void getEvent(final String roomId, final String eventId, final ApiCallback<Event> callback) {
        // try first with roomid / event id
        getEventFromRoomIdEventId(roomId, eventId, new SimpleApiCallback<Event>(callback) {
            @Override
            public void onSuccess(Event event) {
                callback.onSuccess(event);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                if (TextUtils.equals(e.errcode, MatrixError.UNRECOGNIZED)) {
                    // Try to retrieve the event using the context API
                    // It's ok to pass null as a filter here
                    getContextOfEvent(roomId, eventId, 1, null, new SimpleApiCallback<EventContext>(callback) {
                        @Override
                        public void onSuccess(EventContext eventContext) {
                            callback.onSuccess(eventContext.event);
                        }
                    });
                } else {
                    callback.onMatrixError(e);
                }
            }
        });
    }

    /**
     * Retrieve an event from its room id / event id.
     *
     * @param roomId   the room id
     * @param eventId  the event id
     * @param callback the asynchronous callback.
     */
    private void getEventFromRoomIdEventId(final String roomId, final String eventId, final ApiCallback<Event> callback) {
        final String description = "getEventFromRoomIdEventId : roomId " + roomId + " eventId " + eventId;

        mApi.getEvent(roomId, eventId)
                .enqueue(new RestAdapterCallback<Event>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getEventFromRoomIdEventId(roomId, eventId, callback);
                    }
                }));
    }

    /**
     * Get the context surrounding an event.
     *
     * @param roomId          the room id
     * @param eventId         the event Id
     * @param limit           the maximum number of messages to retrieve
     * @param roomEventFilter A RoomEventFilter to filter returned events with. Optional.
     * @param callback        the asynchronous callback called with the response
     */
    public void getContextOfEvent(final String roomId,
                                  final String eventId,
                                  final int limit,
                                  @Nullable final RoomEventFilter roomEventFilter,
                                  final ApiCallback<EventContext> callback) {
        final String description = "getContextOfEvent : roomId " + roomId + " eventId " + eventId + " limit " + limit;

        mApi.getContextOfEvent(roomId, eventId, limit, toJson(roomEventFilter))
                .enqueue(new RestAdapterCallback<EventContext>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getContextOfEvent(roomId, eventId, limit, roomEventFilter, callback);
                    }
                }));
    }

    /**
     * Update the room name.
     *
     * @param roomId   the room id
     * @param name     the room name
     * @param callback the async callback
     */
    public void updateRoomName(final String roomId, final String name, final ApiCallback<Void> callback) {
        final String description = "updateName : roomId " + roomId + " name " + name;

        Map<String, Object> params = new HashMap<>();
        params.put("name", name);

        mApi.sendStateEvent(roomId, Event.EVENT_TYPE_STATE_ROOM_NAME, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateRoomName(roomId, name, callback);
                    }
                }));
    }

    /**
     * Update the canonical alias.
     *
     * @param roomId         the room id
     * @param canonicalAlias the canonical alias
     * @param callback       the async callback
     */
    public void updateCanonicalAlias(final String roomId, final String canonicalAlias, final ApiCallback<Void> callback) {
        final String description = "updateCanonicalAlias : roomId " + roomId + " canonicalAlias " + canonicalAlias;

        Map<String, Object> params = new HashMap<>();
        params.put("alias", canonicalAlias);

        mApi.sendStateEvent(roomId, Event.EVENT_TYPE_STATE_CANONICAL_ALIAS, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateCanonicalAlias(roomId, canonicalAlias, callback);
                    }
                }));
    }

    /**
     * Update history visibility.
     *
     * @param roomId      the room id
     * @param aVisibility the visibility
     * @param callback    the async callback
     */
    public void updateHistoryVisibility(final String roomId, final String aVisibility, final ApiCallback<Void> callback) {
        final String description = "updateHistoryVisibility : roomId " + roomId + " visibility " + aVisibility;

        Map<String, Object> params = new HashMap<>();
        params.put("history_visibility", aVisibility);

        mApi.sendStateEvent(roomId, Event.EVENT_TYPE_STATE_HISTORY_VISIBILITY, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateHistoryVisibility(roomId, aVisibility, callback);
                    }
                }));
    }

    /**
     * Update the directory visibility of the room.
     *
     * @param aRoomId              the room id
     * @param aDirectoryVisibility the visibility of the room in the directory list
     * @param callback             the async callback response
     */
    public void updateDirectoryVisibility(final String aRoomId, final String aDirectoryVisibility, final ApiCallback<Void> callback) {
        final String description = "updateRoomDirectoryVisibility : roomId=" + aRoomId + " visibility=" + aDirectoryVisibility;

        RoomDirectoryVisibility roomDirectoryVisibility = new RoomDirectoryVisibility();
        roomDirectoryVisibility.visibility = aDirectoryVisibility;

        mApi.setRoomDirectoryVisibility(aRoomId, roomDirectoryVisibility)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateDirectoryVisibility(aRoomId, aDirectoryVisibility, callback);
                    }
                }));
    }


    /**
     * Get the directory visibility of the room (see {@link #updateDirectoryVisibility(String, String, ApiCallback)}).
     *
     * @param aRoomId  the room ID
     * @param callback on success callback containing a the room directory visibility
     */
    public void getDirectoryVisibility(final String aRoomId, final ApiCallback<RoomDirectoryVisibility> callback) {
        final String description = "getDirectoryVisibility roomId=" + aRoomId;

        mApi.getRoomDirectoryVisibility(aRoomId)
                .enqueue(new RestAdapterCallback<RoomDirectoryVisibility>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                getDirectoryVisibility(aRoomId, callback);
                            }
                        }));
    }

    /**
     * Get the room members
     *
     * @param roomId        the room id where to get the members
     * @param syncToken     the sync token (optional)
     * @param membership    to include only one type of membership (optional)
     * @param notMembership to exclude one type of membership (optional)
     * @param callback      the callback
     */
    public void getRoomMembers(final String roomId,
                               @Nullable final String syncToken,
                               @Nullable final String membership,
                               @Nullable final String notMembership,
                               final ApiCallback<ChunkEvents> callback) {
        final String description = "getRoomMembers roomId=" + roomId;

        mApi.getMembers(roomId, syncToken, membership, notMembership)
                .enqueue(new RestAdapterCallback<ChunkEvents>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getRoomMembers(roomId, syncToken, membership, notMembership, callback);
                    }
                }));
    }

    /**
     * Update the room topic.
     *
     * @param roomId   the room id
     * @param topic    the room topic
     * @param callback the async callback
     */
    public void updateTopic(final String roomId, final String topic, final ApiCallback<Void> callback) {
        final String description = "updateTopic : roomId " + roomId + " topic " + topic;

        Map<String, Object> params = new HashMap<>();
        params.put("topic", topic);

        mApi.sendStateEvent(roomId, Event.EVENT_TYPE_STATE_ROOM_TOPIC, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateTopic(roomId, topic, callback);
                    }
                }));
    }

    /**
     * Redact an event.
     *
     * @param roomId   the room id
     * @param eventId  the event id
     * @param callback the callback containing the created event if successful
     */
    public void redactEvent(final String roomId, final String eventId, final ApiCallback<Event> callback) {
        final String description = "redactEvent : roomId " + roomId + " eventId " + eventId;

        mApi.redactEvent(roomId, eventId, new JsonObject())
                .enqueue(new RestAdapterCallback<Event>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        redactEvent(roomId, eventId, callback);
                    }
                }));
    }

    /**
     * Report an event.
     *
     * @param roomId   the room id
     * @param eventId  the event id
     * @param score    the metric to let the user rate the severity of the abuse. It ranges from -100 “most offensive” to 0 “inoffensive”
     * @param reason   the reason
     * @param callback the callback
     */
    public void reportEvent(final String roomId, final String eventId, final int score, final String reason, final ApiCallback<Void> callback) {
        final String description = "report : roomId " + roomId + " eventId " + eventId;

        ReportContentParams content = new ReportContentParams();

        content.score = score;
        content.reason = reason;

        mApi.reportEvent(roomId, eventId, content)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        reportEvent(roomId, eventId, score, reason, callback);
                    }
                }));
    }

    /**
     * Update the power levels.
     *
     * @param roomId      the room id
     * @param powerLevels the new powerLevels
     * @param callback    the async callback
     */
    public void updatePowerLevels(final String roomId, final PowerLevels powerLevels, final ApiCallback<Void> callback) {
        final String description = "updatePowerLevels : roomId " + roomId + " powerLevels " + powerLevels;

        mApi.setPowerLevels(roomId, powerLevels)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updatePowerLevels(roomId, powerLevels, callback);
                    }
                }));
    }

    /**
     * Send a state events.
     *
     * @param roomId    the dedicated room id
     * @param eventType the event type
     * @param stateKey  the state key
     * @param params    the put parameters
     * @param callback  the asynchronous callback
     */
    public void sendStateEvent(final String roomId,
                               final String eventType,
                               @Nullable final String stateKey,
                               final Map<String, Object> params,
                               final ApiCallback<Void> callback) {
        final String description = "sendStateEvent : roomId " + roomId + " - eventType " + eventType;

        if (null != stateKey) {
            mApi.sendStateEvent(roomId, eventType, stateKey, params)
                    .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                        @Override
                        public void onRetry() {
                            sendStateEvent(roomId, eventType, stateKey, params, callback);
                        }
                    }));
        } else {
            mApi.sendStateEvent(roomId, eventType, params)
                    .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                        @Override
                        public void onRetry() {
                            sendStateEvent(roomId, eventType, null, params, callback);
                        }
                    }));
        }
    }

    /**
     * Looks up the contents of a state event in a room
     *
     * @param roomId    the room id
     * @param eventType the event type
     * @param callback  the asynchronous callback
     */
    public void getStateEvent(final String roomId, final String eventType, final ApiCallback<JsonElement> callback) {
        final String description = "getStateEvent : roomId " + roomId + " eventId " + eventType;

        mApi.getStateEvent(roomId, eventType)
                .enqueue(new RestAdapterCallback<JsonElement>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getStateEvent(roomId, eventType, callback);
                    }
                }));
    }

    /**
     * Looks up the contents of a state event in a room
     *
     * @param roomId    the room id
     * @param eventType the event type
     * @param stateKey  the key of the state to look up
     * @param callback  the asynchronous callback
     */
    public void getStateEvent(final String roomId, final String eventType, final String stateKey, final ApiCallback<JsonElement> callback) {
        final String description = "getStateEvent : roomId " + roomId + " eventId " + eventType + " stateKey " + stateKey;

        mApi.getStateEvent(roomId, eventType, stateKey)
                .enqueue(new RestAdapterCallback<JsonElement>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        getStateEvent(roomId, eventType, stateKey, callback);
                    }
                }));
    }

    /**
     * send typing notification.
     *
     * @param roomId   the room id
     * @param userId   the user id
     * @param isTyping true if the user is typing
     * @param timeout  the typing event timeout
     * @param callback the asynchronous callback
     */
    public void sendTypingNotification(String roomId, String userId, boolean isTyping, int timeout, ApiCallback<Void> callback) {
        final String description = "sendTypingNotification : roomId " + roomId + " isTyping " + isTyping;

        Typing typing = new Typing();
        typing.typing = isTyping;

        if (-1 != timeout) {
            typing.timeout = timeout;
        }

        // never resend typing on network error
        mApi.setTypingNotification(roomId, userId, typing)
                .enqueue(new RestAdapterCallback<Void>(description, null, callback, null));
    }

    /**
     * Update the room avatar url.
     *
     * @param roomId    the room id
     * @param avatarUrl canonical alias
     * @param callback  the async callback
     */
    public void updateAvatarUrl(final String roomId, final String avatarUrl, final ApiCallback<Void> callback) {
        final String description = "updateAvatarUrl : roomId " + roomId + " avatarUrl " + avatarUrl;

        Map<String, Object> params = new HashMap<>();
        params.put("url", avatarUrl);

        mApi.sendStateEvent(roomId, Event.EVENT_TYPE_STATE_ROOM_AVATAR, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateAvatarUrl(roomId, avatarUrl, callback);
                    }
                }));
    }

    /**
     * Send a read markers.
     *
     * @param roomId    the room id
     * @param rmEventId the read marker event Id
     * @param rrEventId the read receipt event Id
     * @param callback  the callback
     */
    public void sendReadMarker(final String roomId, final String rmEventId, final String rrEventId, final ApiCallback<Void> callback) {
        final String description = "sendReadMarker : roomId " + roomId + " - rmEventId " + rmEventId + " -- rrEventId " + rrEventId;
        final Map<String, String> params = new HashMap<>();

        if (!TextUtils.isEmpty(rmEventId)) {
            params.put(READ_MARKER_FULLY_READ, rmEventId);
        }

        if (!TextUtils.isEmpty(rrEventId)) {
            params.put(READ_MARKER_READ, rrEventId);
        }

        mApi.sendReadMarker(roomId, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, true, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                sendReadMarker(roomId, rmEventId, rrEventId, callback);
                            }
                        }));
    }

    /**
     * Add a tag to a room.
     * Use this method to update the order of an existing tag.
     *
     * @param roomId   the roomId
     * @param tag      the new tag to add to the room.
     * @param order    the order.
     * @param callback the operation callback
     */
    public void addTag(final String roomId, final String tag, final Double order, final ApiCallback<Void> callback) {
        final String description = "addTag : roomId " + roomId + " - tag " + tag + " - order " + order;

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("order", order);

        mApi.addTag(mCredentials.getUserId(), roomId, tag, hashMap)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        addTag(roomId, tag, order, callback);
                    }
                }));
    }

    /**
     * Remove a tag to a room.
     *
     * @param roomId   the roomId
     * @param tag      the new tag to add to the room.
     * @param callback the operation callback
     */
    public void removeTag(final String roomId, final String tag, final ApiCallback<Void> callback) {
        final String description = "removeTag : roomId " + roomId + " - tag " + tag;

        mApi.removeTag(mCredentials.getUserId(), roomId, tag)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        removeTag(roomId, tag, callback);
                    }
                }));
    }

    /**
     * Update the URL preview status
     *
     * @param roomId   the roomId
     * @param status   the new status
     * @param callback the operation callback
     */
    public void updateURLPreviewStatus(final String roomId, final boolean status, final ApiCallback<Void> callback) {
        final String description = "updateURLPreviewStatus : roomId " + roomId + " - status " + status;

        Map<String, Object> params = new HashMap<>();
        params.put(AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE, !status);

        mApi.updateAccountData(mCredentials.getUserId(), roomId, Event.EVENT_TYPE_URL_PREVIEW, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateURLPreviewStatus(roomId, status, callback);
                    }
                }));
    }

    /**
     * Get the room ID corresponding to this room alias.
     *
     * @param roomAlias the room alias.
     * @param callback  the operation callback
     */
    public void getRoomIdByAlias(final String roomAlias, final ApiCallback<RoomAliasDescription> callback) {
        final String description = "getRoomIdByAlias : " + roomAlias;

        mApi.getRoomIdByAlias(roomAlias)
                .enqueue(new RestAdapterCallback<RoomAliasDescription>(description, mUnsentEventsManager, callback,
                        new RestAdapterCallback.RequestRetryCallBack() {
                            @Override
                            public void onRetry() {
                                getRoomIdByAlias(roomAlias, callback);
                            }
                        }));
    }

    /**
     * Set the room ID corresponding to a room alias.
     *
     * @param roomId    the room id.
     * @param roomAlias the room alias.
     * @param callback  the operation callback
     */
    public void setRoomIdByAlias(final String roomId, final String roomAlias, final ApiCallback<Void> callback) {
        final String description = "setRoomIdByAlias : roomAlias " + roomAlias + " - roomId : " + roomId;

        RoomAliasDescription roomAliasDescription = new RoomAliasDescription();
        roomAliasDescription.room_id = roomId;

        mApi.setRoomIdByAlias(roomAlias, roomAliasDescription)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        setRoomIdByAlias(roomId, roomAlias, callback);
                    }
                }));
    }

    /**
     * Remove the room alias.
     *
     * @param roomAlias the room alias.
     * @param callback  the room alias description
     */
    public void removeRoomAlias(final String roomAlias, final ApiCallback<Void> callback) {
        final String description = "removeRoomAlias : " + roomAlias;

        mApi.removeRoomAlias(roomAlias)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        removeRoomAlias(roomAlias, callback);
                    }
                }));
    }

    /**
     * Update the join rule of the room.
     * To make the room private, the aJoinRule must be set to {@link RoomState#JOIN_RULE_INVITE}.
     *
     * @param aRoomId   the room id
     * @param aJoinRule the join rule: {@link RoomState#JOIN_RULE_PUBLIC} or {@link RoomState#JOIN_RULE_INVITE}
     * @param callback  the async callback response
     */
    public void updateJoinRules(final String aRoomId, final String aJoinRule, final ApiCallback<Void> callback) {
        final String description = "updateJoinRules : roomId=" + aRoomId + " rule=" + aJoinRule;

        Map<String, Object> params = new HashMap<>();
        params.put("join_rule", aJoinRule);

        mApi.sendStateEvent(aRoomId, Event.EVENT_TYPE_STATE_ROOM_JOIN_RULES, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateJoinRules(aRoomId, aJoinRule, callback);
                    }
                }));
    }

    /**
     * Update the guest access rule of the room.
     * To deny guest access to the room, aGuestAccessRule must be set to {@link RoomState#GUEST_ACCESS_FORBIDDEN}
     *
     * @param aRoomId          the room id
     * @param aGuestAccessRule the guest access rule: {@link RoomState#GUEST_ACCESS_CAN_JOIN} or {@link RoomState#GUEST_ACCESS_FORBIDDEN}
     * @param callback         the async callback response
     */
    public void updateGuestAccess(final String aRoomId, final String aGuestAccessRule, final ApiCallback<Void> callback) {
        final String description = "updateGuestAccess : roomId=" + aRoomId + " rule=" + aGuestAccessRule;

        Map<String, Object> params = new HashMap<>();
        params.put("guest_access", aGuestAccessRule);

        mApi.sendStateEvent(aRoomId, Event.EVENT_TYPE_STATE_ROOM_GUEST_ACCESS, params)
                .enqueue(new RestAdapterCallback<Void>(description, mUnsentEventsManager, callback, new RestAdapterCallback.RequestRetryCallBack() {
                    @Override
                    public void onRetry() {
                        updateGuestAccess(aRoomId, aGuestAccessRule, callback);
                    }
                }));
    }

    @Nullable
    private String toJson(@Nullable RoomEventFilter roomEventFilter) {
        if (roomEventFilter == null) {
            return null;
        }

        return roomEventFilter.toJSONString();
    }
}
