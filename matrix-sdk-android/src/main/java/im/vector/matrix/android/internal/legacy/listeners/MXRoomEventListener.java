/*
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

package im.vector.matrix.android.internal.legacy.listeners;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.List;

/**
 * A listener which filter event for a specific room
 */
public class MXRoomEventListener extends MXEventListener {

    private static final String LOG_TAG = MXRoomEventListener.class.getSimpleName();

    private final String mRoomId;
    private final IMXEventListener mEventListener;
    private final Room mRoom;

    public MXRoomEventListener(@NonNull Room room,
                               @NonNull IMXEventListener eventListener) {
        mRoom = room;
        mRoomId = room.getRoomId();
        mEventListener = eventListener;
    }

    @Override
    public void onPresenceUpdate(Event event, User user) {
        // Only pass event through if the user is a member of the room
        // FIXME LazyLoading. We cannot rely on getMember nullity anymore
        if (mRoom.getMember(user.user_id) != null) {
            try {
                mEventListener.onPresenceUpdate(event, user);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onPresenceUpdate exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onLiveEvent(Event event, RoomState roomState) {
        // Filter out events for other rooms and events while we are joining (before the room is ready)
        if (TextUtils.equals(mRoomId, event.roomId) && mRoom.isReady()) {
            try {
                mEventListener.onLiveEvent(event, roomState);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onLiveEvent exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onLiveEventsChunkProcessed(String fromToken, String toToken) {
        try {
            mEventListener.onLiveEventsChunkProcessed(fromToken, toToken);
        } catch (Exception e) {
            Log.e(LOG_TAG, "onLiveEventsChunkProcessed exception " + e.getMessage(), e);
        }
    }

    @Override
    public void onEventSentStateUpdated(Event event) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, event.roomId)) {
            try {
                mEventListener.onEventSentStateUpdated(event);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onEventSentStateUpdated exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onEventDecrypted(Event event) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, event.roomId)) {
            try {
                mEventListener.onEventDecrypted(event);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onDecryptedEvent exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onEventSent(final Event event, final String prevEventId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, event.roomId)) {
            try {
                mEventListener.onEventSent(event, prevEventId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onEventSent exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onRoomInternalUpdate(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onRoomInternalUpdate(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onRoomInternalUpdate exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onNotificationCountUpdate(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onNotificationCountUpdate(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onNotificationCountUpdate exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onNewRoom(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onNewRoom(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onNewRoom exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onJoinRoom(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onJoinRoom(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onJoinRoom exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onReceiptEvent(String roomId, List<String> senderIds) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onReceiptEvent(roomId, senderIds);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onReceiptEvent exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onRoomTagEvent(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onRoomTagEvent(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onRoomTagEvent exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onReadMarkerEvent(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onReadMarkerEvent(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onReadMarkerEvent exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onRoomFlush(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onRoomFlush(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onRoomFlush exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onLeaveRoom(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onLeaveRoom(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onLeaveRoom exception " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onRoomKick(String roomId) {
        // Filter out events for other rooms
        if (TextUtils.equals(mRoomId, roomId)) {
            try {
                mEventListener.onRoomKick(roomId);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onRoomKick exception " + e.getMessage(), e);
            }
        }
    }
}
