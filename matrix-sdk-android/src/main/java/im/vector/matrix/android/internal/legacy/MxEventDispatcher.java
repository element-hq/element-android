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

package im.vector.matrix.android.internal.legacy;

import android.os.Looper;
import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.data.MyUser;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.listeners.IMXEventListener;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.BingRule;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.util.MXOsHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dispatcher for MXDataHandler
 * This class store a list of listener and dispatch event to every listener on the Ui Thread
 */
/* package */ class MxEventDispatcher {
    private static final String LOG_TAG = MxEventDispatcher.class.getSimpleName();

    private final MXOsHandler mUiHandler;

    @Nullable
    private IMXEventListener mCryptoEventsListener = null;

    private final Set<IMXEventListener> mEventListeners = new HashSet<>();

    MxEventDispatcher() {
        mUiHandler = new MXOsHandler(Looper.getMainLooper());
    }

    /* ==========================================================================================
     * Public utilities
     * ========================================================================================== */

    /**
     * Set the crypto events listener, or remove it
     *
     * @param listener the listener or null to remove the listener
     */
    public void setCryptoEventsListener(@Nullable IMXEventListener listener) {
        mCryptoEventsListener = listener;
    }

    /**
     * Add a listener to the listeners list.
     *
     * @param listener the listener to add.
     */
    public void addListener(IMXEventListener listener) {
        mEventListeners.add(listener);
    }

    /**
     * Remove a listener from the listeners list.
     *
     * @param listener to remove.
     */
    public void removeListener(IMXEventListener listener) {
        mEventListeners.remove(listener);
    }

    /**
     * Remove any listener
     */
    public void clearListeners() {
        mEventListeners.clear();
    }

    /* ==========================================================================================
     * Dispatchers
     * ========================================================================================== */

    public void dispatchOnStoreReady() {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onStoreReady();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onStoreReady " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnAccountInfoUpdate(final MyUser myUser) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onAccountInfoUpdate(myUser);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onAccountInfoUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnPresenceUpdate(final Event event, final User user) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onPresenceUpdate(event, user);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onPresenceUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnLiveEvent(final Event event, final RoomState roomState) {
        if (null != mCryptoEventsListener) {
            mCryptoEventsListener.onLiveEvent(event, roomState);
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onLiveEvent(event, roomState);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onLiveEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnLiveEventsChunkProcessed(final String startToken, final String toToken) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onLiveEventsChunkProcessed(startToken, toToken);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onLiveEventsChunkProcessed " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnBingEvent(final Event event, final RoomState roomState, final BingRule bingRule, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onBingEvent(event, roomState, bingRule);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onBingEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnEventSentStateUpdated(final Event event, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onEventSentStateUpdated(event);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onEventSentStateUpdated " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnEventSent(final Event event, final String prevEventId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onEventSent(event, prevEventId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onEventSent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnBingRulesUpdate() {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onBingRulesUpdate();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onBingRulesUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnInitialSyncComplete(final String toToken) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onInitialSyncComplete(toToken);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onInitialSyncComplete " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnCryptoSyncComplete() {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onCryptoSyncComplete();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "OnCryptoSyncComplete " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnSyncError(final MatrixError matrixError) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onSyncError(matrixError);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onSyncError " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnNewRoom(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onNewRoom(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onNewRoom " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnJoinRoom(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onJoinRoom(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onJoinRoom " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnRoomInternalUpdate(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onRoomInternalUpdate(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onRoomInternalUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnLeaveRoom(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onLeaveRoom(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onLeaveRoom " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnRoomKick(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onRoomKick(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onRoomKick " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnReceiptEvent(final String roomId, final List<String> senderIds, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onReceiptEvent(roomId, senderIds);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onReceiptEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnRoomTagEvent(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onRoomTagEvent(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onRoomTagEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnReadMarkerEvent(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onReadMarkerEvent(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onReadMarkerEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnRoomFlush(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onRoomFlush(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onRoomFlush " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnIgnoredUsersListUpdate() {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onIgnoredUsersListUpdate();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onIgnoredUsersListUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnToDeviceEvent(final Event event, boolean ignoreEvent) {
        if (null != mCryptoEventsListener) {
            mCryptoEventsListener.onToDeviceEvent(event);
        }

        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onToDeviceEvent(event);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "OnToDeviceEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnDirectMessageChatRoomsListUpdate() {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onDirectMessageChatRoomsListUpdate();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onDirectMessageChatRoomsListUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnEventDecrypted(final Event event) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onEventDecrypted(event);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onDecryptedEvent " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnNewGroupInvitation(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onNewGroupInvitation(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onNewGroupInvitation " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnJoinGroup(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onJoinGroup(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onJoinGroup " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnLeaveGroup(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onLeaveGroup(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onLeaveGroup " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnGroupProfileUpdate(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onGroupProfileUpdate(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onGroupProfileUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnGroupRoomsListUpdate(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onGroupRoomsListUpdate(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onGroupRoomsListUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnGroupUsersListUpdate(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onGroupUsersListUpdate(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onGroupUsersListUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnGroupInvitedUsersListUpdate(final String groupId) {
        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onGroupInvitedUsersListUpdate(groupId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onGroupInvitedUsersListUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    public void dispatchOnNotificationCountUpdate(final String roomId, boolean ignoreEvent) {
        if (ignoreEvent) {
            return;
        }

        final List<IMXEventListener> eventListeners = getListenersSnapshot();

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IMXEventListener listener : eventListeners) {
                    try {
                        listener.onNotificationCountUpdate(roomId);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "onNotificationCountUpdate " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    /* ==========================================================================================
     * Private
     * ========================================================================================== */

    /**
     * @return the current MXEvents listeners.
     */
    private List<IMXEventListener> getListenersSnapshot() {
        List<IMXEventListener> eventListeners;

        synchronized (this) {
            eventListeners = new ArrayList<>(mEventListeners);
        }

        return eventListeners;
    }
}
