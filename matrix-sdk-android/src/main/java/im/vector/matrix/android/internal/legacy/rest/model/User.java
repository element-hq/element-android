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

package im.vector.matrix.android.internal.legacy.rest.model;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.listeners.IMXEventListener;
import im.vector.matrix.android.internal.legacy.listeners.MXEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing a user.
 */
public class User implements java.io.Serializable {
    private static final long serialVersionUID = 5234056937639712713L;

    // the user presence values
    public static final String PRESENCE_ONLINE = "online";
    public static final String PRESENCE_UNAVAILABLE = "unavailable";
    public static final String PRESENCE_OFFLINE = "offline";
    public static final String PRESENCE_FREE_FOR_CHAT = "free_for_chat";
    public static final String PRESENCE_HIDDEN = "hidden";

    // user fields provided by the server
    public String user_id;
    public String displayname;
    public String avatar_url;
    public String presence;
    public Boolean currently_active;
    public Long lastActiveAgo;
    public String statusMsg;

    // tell if the information has been refreshed
    private transient boolean mIsPresenceRefreshed;

    // Used to provide a more realistic last active time:
    // the last active ago time provided by the server + the time that has gone by since
    private long mLastPresenceTs;

    // Map to keep track of the listeners the client adds vs. the ones we actually register to the global data handler.
    // This is needed to find the right one when removing the listener.
    private transient Map<IMXEventListener, IMXEventListener> mEventListeners = new HashMap<>();

    // data handler
    protected transient MXDataHandler mDataHandler;

    // events listeners list
    private transient List<IMXEventListener> mPendingListeners = new ArrayList<>();

    // hash key to store the user in the file system;
    private Integer mStorageHashKey = null;

    // The user data can have been retrieved by a room member
    // The data can be partially invalid until a presence is received
    private boolean mIsRetrievedFromRoomMember = false;

    // avatar URLs setter / getter
    public String getAvatarUrl() {
        return avatar_url;
    }

    public void setAvatarUrl(String newAvatarUrl) {
        avatar_url = newAvatarUrl;
    }

    /**
     * Tells if this user has been created from a room member event
     *
     * @return true if this user has been created from a room member event
     */
    public boolean isRetrievedFromRoomMember() {
        return mIsRetrievedFromRoomMember;
    }

    /**
     * Set that this user has been created from a room member.
     */
    public void setRetrievedFromRoomMember() {
        mIsRetrievedFromRoomMember = true;
    }

    /**
     * Check if mEventListeners has been initialized before providing it.
     * The users are now serialized and the transient fields are not initialized.
     *
     * @return the events listener
     */
    private Map<IMXEventListener, IMXEventListener> getEventListeners() {
        if (null == mEventListeners) {
            mEventListeners = new HashMap<>();
        }

        return mEventListeners;
    }

    /**
     * Check if mPendingListeners has been initialized before providing it.
     * The users are now serialized and the transient fields are not initialized.
     *
     * @return the pending listener
     */
    private List<IMXEventListener> getPendingListeners() {
        if (null == mPendingListeners) {
            mPendingListeners = new ArrayList<>();
        }

        return mPendingListeners;
    }

    /**
     * @return the user hash key
     */
    public int getStorageHashKey() {
        if (null == mStorageHashKey) {
            mStorageHashKey = Math.abs(user_id.hashCode() % 100);
        }

        return mStorageHashKey;
    }

    /**
     * @return true if the presence should be refreshed
     */
    public boolean isPresenceObsolete() {
        return !mIsPresenceRefreshed || (null == presence);
    }

    /**
     * Clone an user into this instance
     *
     * @param user the user to clone.
     */
    protected void clone(User user) {
        if (user != null) {
            user_id = user.user_id;
            displayname = user.displayname;
            avatar_url = user.avatar_url;
            presence = user.presence;
            currently_active = user.currently_active;
            lastActiveAgo = user.lastActiveAgo;
            statusMsg = user.statusMsg;

            mIsPresenceRefreshed = user.mIsPresenceRefreshed;
            mLastPresenceTs = user.mLastPresenceTs;

            mEventListeners = new HashMap<>(user.getEventListeners());
            mDataHandler = user.mDataHandler;

            mPendingListeners = user.getPendingListeners();
        }
    }

    /**
     * Create a deep copy of the current user.
     *
     * @return a deep copy of the current object
     */
    public User deepCopy() {
        User copy = new User();
        copy.clone(this);
        return copy;
    }

    /**
     * Tells if an user is active
     *
     * @return true if the user is active
     */
    public boolean isActive() {
        return TextUtils.equals(presence, PRESENCE_ONLINE) || ((null != currently_active) && currently_active);
    }

    /**
     * Set the latest presence event time.
     *
     * @param ts the timestamp.
     */
    public void setLatestPresenceTs(long ts) {
        mIsPresenceRefreshed = true;
        mLastPresenceTs = ts;
    }

    /**
     * @return the timestamp of the latest presence event.
     */
    public long getLatestPresenceTs() {
        return mLastPresenceTs;
    }

    /**
     * Get the user's last active ago time by adding the one given by the server and the time since elapsed.
     *
     * @return how long ago the user was last active (in ms)
     */
    public long getAbsoluteLastActiveAgo() {
        // sanity check
        if (null == lastActiveAgo) {
            return 0;
        } else {
            return System.currentTimeMillis() - (mLastPresenceTs - lastActiveAgo);
        }
    }

    /**
     * Set the event listener to send back events to. This is typically the DataHandler for dispatching the events to listeners.
     *
     * @param dataHandler should be the main data handler for dispatching back events to registered listeners.
     */
    public void setDataHandler(MXDataHandler dataHandler) {
        mDataHandler = dataHandler;

        for (IMXEventListener listener : getPendingListeners()) {
            mDataHandler.addListener(listener);
        }
    }

    /**
     * Add an event listener to this room. Only events relative to the room will come down.
     *
     * @param eventListener the event listener to add
     */
    public void addEventListener(final IMXEventListener eventListener) {
        // Create a global listener that we'll add to the data handler
        IMXEventListener globalListener = new MXEventListener() {
            @Override
            public void onPresenceUpdate(Event event, User user) {
                // Only pass event through for this user
                if (user.user_id.equals(user_id)) {
                    eventListener.onPresenceUpdate(event, user);
                }
            }
        };
        getEventListeners().put(eventListener, globalListener);

        // the handler could be set later
        if (null != mDataHandler) {
            mDataHandler.addListener(globalListener);
        } else {
            getPendingListeners().add(globalListener);
        }
    }

    /**
     * Remove an event listener.
     *
     * @param eventListener the event listener to remove
     */
    public void removeEventListener(IMXEventListener eventListener) {

        if (null != mDataHandler) {
            mDataHandler.removeListener(getEventListeners().get(eventListener));
        } else {
            getPendingListeners().remove(getEventListeners().get(eventListener));
        }

        getEventListeners().remove(eventListener);
    }
}
