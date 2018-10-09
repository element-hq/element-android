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
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.HomeServerConnectionConfig;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomAccountData;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.ReceiptData;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.group.Group;
import im.vector.matrix.android.internal.legacy.rest.model.pid.ThirdPartyIdentifier;
import im.vector.matrix.android.internal.legacy.util.CompatUtil;
import im.vector.matrix.android.internal.legacy.util.ContentUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.util.MXOsHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * An in-file IMXStore.
 */
public class MXFileStore extends MXMemoryStore {
    private static final String LOG_TAG = MXFileStore.class.getSimpleName();

    // some constant values
    private static final int MXFILE_VERSION = 22;

    // ensure that there is enough messages to fill a tablet screen
    private static final int MAX_STORED_MESSAGES_COUNT = 50;

    private static final String MXFILE_STORE_FOLDER = "MXFileStore";
    private static final String MXFILE_STORE_METADATA_FILE_NAME = "MXFileStore";

    private static final String MXFILE_STORE_GZ_ROOMS_MESSAGES_FOLDER = "messages_gz";
    private static final String MXFILE_STORE_ROOMS_TOKENS_FOLDER = "tokens";
    private static final String MXFILE_STORE_GZ_ROOMS_STATE_FOLDER = "state_gz";
    private static final String MXFILE_STORE_GZ_ROOMS_STATE_EVENTS_FOLDER = "state_rooms_events";
    private static final String MXFILE_STORE_ROOMS_SUMMARY_FOLDER = "summary";
    private static final String MXFILE_STORE_ROOMS_RECEIPT_FOLDER = "receipts";
    private static final String MXFILE_STORE_ROOMS_ACCOUNT_DATA_FOLDER = "accountData";
    private static final String MXFILE_STORE_USER_FOLDER = "users";
    private static final String MXFILE_STORE_GROUPS_FOLDER = "groups";

    // the data is read from the file system
    private boolean mIsReady = false;

    // tell if the post processing has been done
    private boolean mIsPostProcessingDone = false;

    // the read receipts are ready
    private boolean mAreReceiptsReady = false;

    // the store is currently opening
    private boolean mIsOpening = false;

    // List of rooms to save on [MXStore commit]
    // filled with roomId
    private Set<String> mRoomsToCommitForMessages;
    private Set<String> mRoomsToCommitForStates;
    //private Set<String> mRoomsToCommitForStatesEvents;
    private Set<String> mRoomsToCommitForSummaries;
    private Set<String> mRoomsToCommitForAccountData;
    private Set<String> mRoomsToCommitForReceipts;
    private Set<String> mUserIdsToCommit;
    private Set<String> mGroupsToCommit;

    // Flag to indicate metaData needs to be store
    private boolean mMetaDataHasChanged = false;

    // The path of the MXFileStore folders
    private File mStoreFolderFile = null;
    private File mGzStoreRoomsMessagesFolderFile = null;
    private File mStoreRoomsTokensFolderFile = null;
    private File mGzStoreRoomsStateFolderFile = null;
    private File mGzStoreRoomsStateEventsFolderFile = null;
    private File mStoreRoomsSummaryFolderFile = null;
    private File mStoreRoomsMessagesReceiptsFolderFile = null;
    private File mStoreRoomsAccountDataFolderFile = null;
    private File mStoreUserFolderFile = null;
    private File mStoreGroupsFolderFile = null;

    // the background thread
    private HandlerThread mHandlerThread = null;
    private MXOsHandler mFileStoreHandler = null;

    private boolean mIsKilled = false;

    private boolean mIsNewStorage = false;

    private boolean mAreUsersLoaded = false;

    private long mPreloadTime = 0;

    // the read receipts are asynchronously loaded
    // keep a list of the remaining receipts to load
    private final List<String> mRoomReceiptsToLoad = new ArrayList<>();

    // store some stats
    private final Map<String, Long> mStoreStats = new HashMap<>();

    // True if file encryption is enabled
    private final boolean mEnableFileEncryption;

    /**
     * Create the file store dirtrees
     */
    private void createDirTree(String userId) {
        // data path
        // MXFileStore/userID/
        // MXFileStore/userID/MXFileStore
        // MXFileStore/userID/MXFileStore/Messages/
        // MXFileStore/userID/MXFileStore/Tokens/
        // MXFileStore/userID/MXFileStore/States/
        // MXFileStore/userID/MXFileStore/Summaries/
        // MXFileStore/userID/MXFileStore/receipt/<room Id>/receipts
        // MXFileStore/userID/MXFileStore/accountData/
        // MXFileStore/userID/MXFileStore/users/
        // MXFileStore/userID/MXFileStore/groups/

        // create the dirtree
        mStoreFolderFile = new File(new File(mContext.getApplicationContext().getFilesDir(), MXFILE_STORE_FOLDER), userId);

        if (!mStoreFolderFile.exists()) {
            mStoreFolderFile.mkdirs();
        }

        mGzStoreRoomsMessagesFolderFile = new File(mStoreFolderFile, MXFILE_STORE_GZ_ROOMS_MESSAGES_FOLDER);
        if (!mGzStoreRoomsMessagesFolderFile.exists()) {
            mGzStoreRoomsMessagesFolderFile.mkdirs();
        }

        mStoreRoomsTokensFolderFile = new File(mStoreFolderFile, MXFILE_STORE_ROOMS_TOKENS_FOLDER);
        if (!mStoreRoomsTokensFolderFile.exists()) {
            mStoreRoomsTokensFolderFile.mkdirs();
        }

        mGzStoreRoomsStateFolderFile = new File(mStoreFolderFile, MXFILE_STORE_GZ_ROOMS_STATE_FOLDER);
        if (!mGzStoreRoomsStateFolderFile.exists()) {
            mGzStoreRoomsStateFolderFile.mkdirs();
        }

        mGzStoreRoomsStateEventsFolderFile = new File(mStoreFolderFile, MXFILE_STORE_GZ_ROOMS_STATE_EVENTS_FOLDER);
        if (!mGzStoreRoomsStateEventsFolderFile.exists()) {
            mGzStoreRoomsStateEventsFolderFile.mkdirs();
        }

        mStoreRoomsSummaryFolderFile = new File(mStoreFolderFile, MXFILE_STORE_ROOMS_SUMMARY_FOLDER);
        if (!mStoreRoomsSummaryFolderFile.exists()) {
            mStoreRoomsSummaryFolderFile.mkdirs();
        }

        mStoreRoomsMessagesReceiptsFolderFile = new File(mStoreFolderFile, MXFILE_STORE_ROOMS_RECEIPT_FOLDER);
        if (!mStoreRoomsMessagesReceiptsFolderFile.exists()) {
            mStoreRoomsMessagesReceiptsFolderFile.mkdirs();
        }

        mStoreRoomsAccountDataFolderFile = new File(mStoreFolderFile, MXFILE_STORE_ROOMS_ACCOUNT_DATA_FOLDER);
        if (!mStoreRoomsAccountDataFolderFile.exists()) {
            mStoreRoomsAccountDataFolderFile.mkdirs();
        }

        mStoreUserFolderFile = new File(mStoreFolderFile, MXFILE_STORE_USER_FOLDER);
        if (!mStoreUserFolderFile.exists()) {
            mStoreUserFolderFile.mkdirs();
        }

        mStoreGroupsFolderFile = new File(mStoreFolderFile, MXFILE_STORE_GROUPS_FOLDER);
        if (!mStoreGroupsFolderFile.exists()) {
            mStoreGroupsFolderFile.mkdirs();
        }
    }

    /**
     * Constructor
     *
     * @param hsConfig             the expected credentials
     * @param enableFileEncryption set to true to enable file encryption.
     * @param context              the context.
     */
    public MXFileStore(HomeServerConnectionConfig hsConfig, boolean enableFileEncryption, Context context) {
        setContext(context);

        mEnableFileEncryption = enableFileEncryption;

        mIsReady = false;
        mCredentials = hsConfig.getCredentials();

        mHandlerThread = new HandlerThread("MXFileStoreBackgroundThread_" + mCredentials.userId, Thread.MIN_PRIORITY);

        createDirTree(mCredentials.userId);

        // updated data
        mRoomsToCommitForMessages = new HashSet<>();
        mRoomsToCommitForStates = new HashSet<>();
        //mRoomsToCommitForStatesEvents = new HashSet<>();
        mRoomsToCommitForSummaries = new HashSet<>();
        mRoomsToCommitForAccountData = new HashSet<>();
        mRoomsToCommitForReceipts = new HashSet<>();
        mUserIdsToCommit = new HashSet<>();
        mGroupsToCommit = new HashSet<>();

        // check if the metadata file exists and if it is valid
        loadMetaData();

        if (null == mMetadata) {
            deleteAllData(true);
        }

        // create the metadata file if it does not exist
        // either there is no store
        // or the store was not properly initialised (the application crashed during the initialsync)
        if ((null == mMetadata) || (null == mMetadata.mAccessToken)) {
            mIsNewStorage = true;
            mIsOpening = true;
            mHandlerThread.start();
            mFileStoreHandler = new MXOsHandler(mHandlerThread.getLooper());

            mMetadata = new MXFileStoreMetaData();
            mMetadata.mUserId = mCredentials.userId;
            mMetadata.mAccessToken = mCredentials.accessToken;
            mMetadata.mVersion = MXFILE_VERSION;
            mMetaDataHasChanged = true;
            saveMetaData();

            mEventStreamToken = null;

            mIsOpening = false;
            // nothing to load so ready to work
            mIsReady = true;
            mAreReceiptsReady = true;
        }
    }

    /**
     * Killed the background thread.
     *
     * @param isKilled killed status
     */
    private void setIsKilled(boolean isKilled) {
        synchronized (this) {
            mIsKilled = isKilled;
        }
    }

    /**
     * @return true if the background thread is killed.
     */
    private boolean isKilled() {
        boolean isKilled;

        synchronized (this) {
            isKilled = mIsKilled;
        }

        return isKilled;
    }

    /**
     * Save changes in the store.
     * If the store uses permanent storage like database or file, it is the optimised time
     * to commit the last changes.
     */
    @Override
    public void commit() {
        // Save data only if metaData exists
        if ((null != mMetadata) && (null != mMetadata.mAccessToken) && !isKilled()) {
            Log.d(LOG_TAG, "++ Commit");
            saveUsers();
            saveGroups();
            saveRoomsMessages();
            saveRoomStates();
            saveRoomStatesEvents();
            saveSummaries();
            saveRoomsAccountData();
            saveReceipts();
            saveMetaData();
            Log.d(LOG_TAG, "-- Commit");
        }
    }

    /**
     * Open the store.
     */
    @Override
    public void open() {
        super.open();
        final long fLoadTimeT0 = System.currentTimeMillis();

        // avoid concurrency call.
        synchronized (this) {
            if (!mIsReady && !mIsOpening && (null != mMetadata) && (null != mHandlerThread)) {
                mIsOpening = true;

                Log.e(LOG_TAG, "Open the store.");

                // creation the background handler.
                if (null == mFileStoreHandler) {
                    // avoid already started exception
                    // never succeeded to reproduce but it was reported in GA.
                    try {
                        mHandlerThread.start();
                    } catch (IllegalThreadStateException e) {
                        Log.e(LOG_TAG, "mHandlerThread is already started.", e);
                        // already started
                        return;
                    }
                    mFileStoreHandler = new MXOsHandler(mHandlerThread.getLooper());
                }

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        mFileStoreHandler.post(new Runnable() {
                            public void run() {
                                Log.e(LOG_TAG, "Open the store in the background thread.");

                                String errorDescription = null;
                                boolean succeed = (mMetadata.mVersion == MXFILE_VERSION)
                                        && TextUtils.equals(mMetadata.mUserId, mCredentials.userId)
                                        && TextUtils.equals(mMetadata.mAccessToken, mCredentials.accessToken);

                                if (!succeed) {
                                    errorDescription = "Invalid store content";
                                    Log.e(LOG_TAG, errorDescription);
                                }

                                if (succeed) {
                                    succeed &= loadRoomsMessages();
                                    if (!succeed) {
                                        errorDescription = "loadRoomsMessages fails";
                                        Log.e(LOG_TAG, errorDescription);
                                    } else {
                                        Log.d(LOG_TAG, "loadRoomsMessages succeeds");
                                    }
                                }

                                if (succeed) {
                                    succeed &= loadGroups();
                                    if (!succeed) {
                                        errorDescription = "loadGroups fails";
                                        Log.e(LOG_TAG, errorDescription);
                                    } else {
                                        Log.d(LOG_TAG, "loadGroups succeeds");
                                    }
                                }

                                if (succeed) {
                                    succeed &= loadRoomsState();

                                    if (!succeed) {
                                        errorDescription = "loadRoomsState fails";
                                        Log.e(LOG_TAG, errorDescription);
                                    } else {
                                        Log.d(LOG_TAG, "loadRoomsState succeeds");
                                        long t0 = System.currentTimeMillis();
                                        Log.d(LOG_TAG, "Retrieve the users from the roomstate");

                                        Collection<Room> rooms = getRooms();

                                        for (Room room : rooms) {
                                            Collection<RoomMember> members = room.getState().getLoadedMembers();
                                            for (RoomMember member : members) {
                                                updateUserWithRoomMemberEvent(member);
                                            }
                                        }

                                        long delta = System.currentTimeMillis() - t0;
                                        Log.d(LOG_TAG, "Retrieve " + mUsers.size() + " users with the room states in " + delta + "  ms");
                                        mStoreStats.put("Retrieve users", delta);
                                    }
                                }

                                if (succeed) {
                                    succeed &= loadSummaries();

                                    if (!succeed) {
                                        errorDescription = "loadSummaries fails";
                                        Log.e(LOG_TAG, errorDescription);
                                    } else {
                                        Log.d(LOG_TAG, "loadSummaries succeeds");

                                        // Check if the room summaries match to existing rooms.
                                        // We could have more rooms than summaries because
                                        // some of them are hidden.
                                        // For example, the conference calls create a dummy room to manage
                                        // the call events.
                                        // check also if the user is a member of the room
                                        // https://github.com/vector-im/riot-android/issues/1302

                                        for (String roomId : mRoomSummaries.keySet()) {
                                            Room room = getRoom(roomId);

                                            if (null == room) {
                                                succeed = false;
                                                Log.e(LOG_TAG, "loadSummaries : the room " + roomId + " does not exist");
                                            } else if (null == room.getMember(mCredentials.userId)) {
                                                //succeed = false;
                                                Log.e(LOG_TAG, "loadSummaries) : a summary exists for the roomId "
                                                        + roomId + " but the user is not anymore a member");
                                            }
                                        }
                                    }
                                }

                                if (succeed) {
                                    succeed &= loadRoomsAccountData();

                                    if (!succeed) {
                                        errorDescription = "loadRoomsAccountData fails";
                                        Log.e(LOG_TAG, errorDescription);
                                    } else {
                                        Log.d(LOG_TAG, "loadRoomsAccountData succeeds");
                                    }
                                }

                                // do not expect having empty list
                                // assume that something is corrupted
                                if (!succeed) {
                                    Log.e(LOG_TAG, "Fail to open the store in background");

                                    // delete all data set mMetadata to null
                                    // backup it to restore it
                                    // the behaviour should be the same as first login
                                    MXFileStoreMetaData tmpMetadata = mMetadata;

                                    deleteAllData(true);

                                    mRoomsToCommitForMessages = new HashSet<>();
                                    mRoomsToCommitForStates = new HashSet<>();
                                    //mRoomsToCommitForStatesEvents = new HashSet<>();
                                    mRoomsToCommitForSummaries = new HashSet<>();
                                    mRoomsToCommitForReceipts = new HashSet<>();

                                    mMetadata = tmpMetadata;

                                    // reported by GA
                                    // i don't see which path could have triggered this issue
                                    // mMetadata should only be null at file store loading
                                    if (null == mMetadata) {
                                        mMetadata = new MXFileStoreMetaData();
                                        mMetadata.mUserId = mCredentials.userId;
                                        mMetadata.mAccessToken = mCredentials.accessToken;
                                        mMetaDataHasChanged = true;
                                    } else {
                                        mMetadata.mEventStreamToken = null;
                                    }
                                    mMetadata.mVersion = MXFILE_VERSION;

                                    //  the event stream token is put to zero to ensure ta
                                    mEventStreamToken = null;
                                    mAreReceiptsReady = true;
                                } else {
                                    Log.d(LOG_TAG, "++ store stats");
                                    Set<String> roomIds = mRoomEvents.keySet();

                                    for (String roomId : roomIds) {
                                        Room room = getRoom(roomId);

                                        if ((null != room) && (null != room.getState())) {
                                            int membersCount = room.getState().getLoadedMembers().size();
                                            int eventsCount = mRoomEvents.get(roomId).size();

                                            Log.d(LOG_TAG, " room " + roomId
                                                    + " : (lazy loaded) membersCount " + membersCount
                                                    + " - eventsCount " + eventsCount);
                                        }
                                    }

                                    Log.d(LOG_TAG, "-- store stats");
                                }

                                // post processing
                                Log.d(LOG_TAG, "## open() : post processing.");
                                dispatchPostProcess(mCredentials.userId);
                                mIsPostProcessingDone = true;

                                synchronized (this) {
                                    mIsReady = true;
                                }
                                mIsOpening = false;

                                if (!succeed && !mIsNewStorage) {
                                    Log.e(LOG_TAG, "The store is corrupted.");
                                    dispatchOnStoreCorrupted(mCredentials.userId, errorDescription);
                                } else {
                                    // extract the room states
                                    mRoomReceiptsToLoad.addAll(listFiles(mStoreRoomsMessagesReceiptsFolderFile.list()));
                                    mPreloadTime = System.currentTimeMillis() - fLoadTimeT0;
                                    if (mMetricsListener != null) {
                                        mMetricsListener.onStorePreloaded(mPreloadTime);
                                    }

                                    Log.d(LOG_TAG, "The store is opened.");
                                    dispatchOnStoreReady(mCredentials.userId);

                                    // load the following items with delay
                                    // theses items are not required to be ready

                                    // load the receipts
                                    loadReceipts();

                                    // load the users
                                    loadUsers();
                                }
                            }
                        });
                    }
                };

                Thread t = new Thread(r);
                t.start();
            } else if (mIsReady) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        mFileStoreHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // should never happen
                                if (!mIsPostProcessingDone && !mIsNewStorage) {
                                    Log.e(LOG_TAG, "## open() : is ready but the post processing was not yet done : please wait....");
                                    return;
                                } else {
                                    if (!mIsPostProcessingDone) {
                                        Log.e(LOG_TAG, "## open() : is ready but the post processing was not yet done.");
                                        dispatchPostProcess(mCredentials.userId);
                                        mIsPostProcessingDone = true;
                                    } else {
                                        Log.e(LOG_TAG, "## open() when ready : the post processing is already done.");
                                    }
                                    dispatchOnStoreReady(mCredentials.userId);
                                    mPreloadTime = System.currentTimeMillis() - fLoadTimeT0;
                                    if (mMetricsListener != null) {
                                        mMetricsListener.onStorePreloaded(mPreloadTime);
                                    }
                                }
                            }
                        });
                    }
                };

                Thread t = new Thread(r);
                t.start();
            }

        }
    }

    /**
     * Check if the read receipts are ready to be used.
     *
     * @return true if they are ready.
     */
    @Override
    public boolean areReceiptsReady() {
        boolean res;

        synchronized (this) {
            res = mAreReceiptsReady;
        }

        return res;
    }

    /**
     * Provides the store preload time in milliseconds.
     *
     * @return the store preload time in milliseconds.
     */
    @Override
    public long getPreloadTime() {
        return mPreloadTime;
    }

    /**
     * Provides some store stats
     *
     * @return the store stats
     */
    public Map<String, Long> getStats() {
        return mStoreStats;
    }

    /**
     * Close the store.
     * Any pending operation must be complete in this call.
     */
    @Override
    public void close() {
        Log.d(LOG_TAG, "Close the store");

        super.close();
        setIsKilled(true);
        if (null != mHandlerThread) {
            mHandlerThread.quit();
        }
        mHandlerThread = null;
    }

    /**
     * Clear the store.
     * Any pending operation must be complete in this call.
     */
    @Override
    public void clear() {
        Log.d(LOG_TAG, "Clear the store");
        super.clear();
        deleteAllData(false);
    }

    /**
     * Clear the filesystem storage.
     *
     * @param init true to init the filesystem dirtree
     */
    private void deleteAllData(boolean init) {
        // delete the dedicated directories
        try {
            ContentUtils.deleteDirectory(mStoreFolderFile);
            if (init) {
                createDirTree(mCredentials.userId);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "deleteAllData failed " + e.getMessage(), e);
        }

        if (init) {
            initCommon();
        }
        mMetadata = null;
        mEventStreamToken = null;
        mAreUsersLoaded = true;
    }

    /**
     * Indicate if the MXStore implementation stores data permanently.
     * Permanent storage allows the SDK to make less requests at the startup.
     *
     * @return true if permanent.
     */
    @Override
    public boolean isPermanent() {
        return true;
    }

    /**
     * Check if the initial load is performed.
     *
     * @return true if it is ready.
     */
    @Override
    public boolean isReady() {
        synchronized (this) {
            return mIsReady;
        }
    }

    /**
     * @return true if the store is corrupted.
     */
    @Override
    public boolean isCorrupted() {
        return false;
    }

    /**
     * Delete a directory with its content
     *
     * @param directory the base directory
     * @return the cache file size
     */
    private long directorySize(File directory) {
        long directorySize = 0;

        if (directory.exists()) {
            File[] files = directory.listFiles();

            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        directorySize += directorySize(files[i]);
                    } else {
                        directorySize += files[i].length();
                    }
                }
            }
        }

        return directorySize;
    }

    /**
     * Returns to disk usage size in bytes.
     *
     * @return disk usage size
     */
    @Override
    public long diskUsage() {
        return directorySize(mStoreFolderFile);
    }

    /**
     * Set the event stream token.
     *
     * @param token the event stream token
     */
    @Override
    public void setEventStreamToken(String token) {
        Log.d(LOG_TAG, "Set token to " + token);
        super.setEventStreamToken(token);
        mMetaDataHasChanged = true;
    }

    @Override
    public boolean setDisplayName(String displayName, long ts) {
        return mMetaDataHasChanged = super.setDisplayName(displayName, ts);
    }

    @Override
    public boolean setAvatarURL(String avatarURL, long ts) {
        return mMetaDataHasChanged = super.setAvatarURL(avatarURL, ts);
    }

    @Override
    public void setThirdPartyIdentifiers(List<ThirdPartyIdentifier> identifiers) {
        // privacy
        //Log.d(LOG_TAG, "Set setThirdPartyIdentifiers to " + identifiers);
        Log.d(LOG_TAG, "Set setThirdPartyIdentifiers");
        mMetaDataHasChanged = true;
        super.setThirdPartyIdentifiers(identifiers);
    }

    @Override
    public void setIgnoredUserIdsList(List<String> users) {
        Log.d(LOG_TAG, "## setIgnoredUsers() : " + users);
        mMetaDataHasChanged = true;
        super.setIgnoredUserIdsList(users);
    }

    @Override
    public void setDirectChatRoomsDict(Map<String, List<String>> directChatRoomsDict) {
        Log.d(LOG_TAG, "## setDirectChatRoomsDict()");
        mMetaDataHasChanged = true;
        super.setDirectChatRoomsDict(directChatRoomsDict);
    }

    @Override
    public void storeUser(User user) {
        if (!TextUtils.equals(mCredentials.userId, user.user_id)) {
            mUserIdsToCommit.add(user.user_id);
        }
        super.storeUser(user);
    }

    @Override
    public void flushRoomEvents(String roomId) {
        super.flushRoomEvents(roomId);

        mRoomsToCommitForMessages.add(roomId);

        if ((null != mMetadata) && (null != mMetadata.mAccessToken) && !isKilled()) {
            saveRoomsMessages();
        }
    }

    @Override
    public void storeRoomEvents(String roomId, TokensChunkEvents tokensChunkEvents, EventTimeline.Direction direction) {
        boolean canStore = true;

        // do not flush the room messages file
        // when the user reads the room history and the events list size reaches its max size.
        if (direction == EventTimeline.Direction.BACKWARDS) {
            LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

            if (null != events) {
                canStore = (events.size() < MAX_STORED_MESSAGES_COUNT);

                if (!canStore) {
                    Log.d(LOG_TAG, "storeRoomEvents : do not flush because reaching the max size");
                }
            }
        }

        super.storeRoomEvents(roomId, tokensChunkEvents, direction);

        if (canStore) {
            mRoomsToCommitForMessages.add(roomId);
        }
    }

    /**
     * Store a live room event.
     *
     * @param event The event to be stored.
     */
    @Override
    public void storeLiveRoomEvent(Event event) {
        super.storeLiveRoomEvent(event);
        mRoomsToCommitForMessages.add(event.roomId);
    }

    @Override
    public void deleteEvent(Event event) {
        super.deleteEvent(event);
        mRoomsToCommitForMessages.add(event.roomId);
    }

    /**
     * Delete the room messages and token files.
     *
     * @param roomId the room id.
     */
    private void deleteRoomMessagesFiles(String roomId) {
        // messages list
        File messagesListFile = new File(mGzStoreRoomsMessagesFolderFile, roomId);

        // remove the files
        if (messagesListFile.exists()) {
            try {
                messagesListFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteRoomMessagesFiles - messagesListFile failed " + e.getMessage(), e);
            }
        }

        File tokenFile = new File(mStoreRoomsTokensFolderFile, roomId);
        if (tokenFile.exists()) {
            try {
                tokenFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteRoomMessagesFiles - tokenFile failed " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void deleteRoom(String roomId) {
        Log.d(LOG_TAG, "deleteRoom " + roomId);

        super.deleteRoom(roomId);
        deleteRoomMessagesFiles(roomId);
        deleteRoomStateFile(roomId);
        deleteRoomSummaryFile(roomId);
        deleteRoomReceiptsFile(roomId);
        deleteRoomAccountDataFile(roomId);
    }

    @Override
    public void deleteAllRoomMessages(String roomId, boolean keepUnsent) {
        Log.d(LOG_TAG, "deleteAllRoomMessages " + roomId);

        super.deleteAllRoomMessages(roomId, keepUnsent);
        if (!keepUnsent) {
            deleteRoomMessagesFiles(roomId);
        }

        deleteRoomSummaryFile(roomId);

        mRoomsToCommitForMessages.add(roomId);
        mRoomsToCommitForSummaries.add(roomId);
    }

    @Override
    public void storeLiveStateForRoom(String roomId) {
        super.storeLiveStateForRoom(roomId);
        mRoomsToCommitForStates.add(roomId);
    }

    //================================================================================
    // Summary management
    //================================================================================

    @Override
    public void flushSummary(RoomSummary summary) {
        super.flushSummary(summary);
        mRoomsToCommitForSummaries.add(summary.getRoomId());

        if ((null != mMetadata) && (null != mMetadata.mAccessToken) && !isKilled()) {
            saveSummaries();
        }
    }

    @Override
    public void flushSummaries() {
        super.flushSummaries();

        // add any existing roomid to the list to save all
        mRoomsToCommitForSummaries.addAll(mRoomSummaries.keySet());

        if ((null != mMetadata) && (null != mMetadata.mAccessToken) && !isKilled()) {
            saveSummaries();
        }
    }

    @Override
    public void storeSummary(RoomSummary summary) {
        super.storeSummary(summary);

        if ((null != summary) && (null != summary.getRoomId()) && !mRoomsToCommitForSummaries.contains(summary.getRoomId())) {
            mRoomsToCommitForSummaries.add(summary.getRoomId());
        }
    }

    //================================================================================
    // users management
    //================================================================================

    /**
     * Flush users list
     */
    private void saveUsers() {
        if (!mAreUsersLoaded) {
            // please wait
            return;
        }

        // some updated rooms ?
        if ((mUserIdsToCommit.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fUserIds = mUserIdsToCommit;
            mUserIdsToCommit = new HashSet<>();

            try {
                final Set<User> fUsers;

                synchronized (mUsers) {
                    fUsers = new HashSet<>(mUsers.values());
                }

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        mFileStoreHandler.post(new Runnable() {
                            public void run() {
                                if (!isKilled()) {
                                    Log.d(LOG_TAG, "saveUsers " + fUserIds.size() + " users (" + fUsers.size() + " known ones)");

                                    long start = System.currentTimeMillis();

                                    // the users are split into groups to save time
                                    Map<Integer, List<User>> usersGroups = new HashMap<>();

                                    // finds the group for each updated user
                                    for (String userId : fUserIds) {
                                        User user;

                                        synchronized (mUsers) {
                                            user = mUsers.get(userId);
                                        }

                                        if (null != user) {
                                            int hashCode = user.getStorageHashKey();

                                            if (!usersGroups.containsKey(hashCode)) {
                                                usersGroups.put(hashCode, new ArrayList<User>());
                                            }
                                        }
                                    }

                                    // gather the user to the dedicated group if they need to be updated
                                    for (User user : fUsers) {
                                        if (usersGroups.containsKey(user.getStorageHashKey())) {
                                            usersGroups.get(user.getStorageHashKey()).add(user);
                                        }
                                    }

                                    // save the groups
                                    for (int hashKey : usersGroups.keySet()) {
                                        writeObject("saveUser " + hashKey, new File(mStoreUserFolderFile, hashKey + ""), usersGroups.get(hashKey));
                                    }

                                    Log.d(LOG_TAG, "saveUsers done in " + (System.currentTimeMillis() - start) + " ms");
                                }
                            }
                        });
                    }
                };

                Thread t = new Thread(r);
                t.start();
            } catch (OutOfMemoryError oom) {
                Log.e(LOG_TAG, "saveUser : cannot clone the users list" + oom.getMessage(), oom);
            }
        }
    }

    /**
     * Load the user information from the filesystem..
     */
    private void loadUsers() {
        List<String> filenames = listFiles(mStoreUserFolderFile.list());
        long start = System.currentTimeMillis();

        List<User> users = new ArrayList<>();

        // list the files
        for (String filename : filenames) {
            File messagesListFile = new File(mStoreUserFolderFile, filename);
            Object usersAsVoid = readObject("loadUsers " + filename, messagesListFile);

            if (null != usersAsVoid) {
                try {
                    users.addAll((List<User>) usersAsVoid);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "loadUsers failed : " + e.toString(), e);
                }
            }
        }

        // update the hash map
        for (User user : users) {
            synchronized (mUsers) {
                User currentUser = mUsers.get(user.user_id);

                if ((null == currentUser) || // not defined
                        currentUser.isRetrievedFromRoomMember() || // tmp user until retrieved it
                        (currentUser.getLatestPresenceTs() < user.getLatestPresenceTs())) // newer presence
                {
                    mUsers.put(user.user_id, user);
                }
            }
        }

        long delta = (System.currentTimeMillis() - start);
        Log.e(LOG_TAG, "loadUsers (" + filenames.size() + " files) : retrieve " + mUsers.size() + " users in " + delta + "ms");
        mStoreStats.put("loadUsers", delta);

        mAreUsersLoaded = true;

        // save any pending save
        saveUsers();
    }

    //================================================================================
    // Room messages management
    //================================================================================

    /**
     * Computes the saved events map to reduce storage footprint.
     *
     * @param roomId the room id
     * @return the saved eventMap
     */
    private LinkedHashMap<String, Event> getSavedEventsMap(String roomId) {
        LinkedHashMap<String, Event> eventsMap;

        synchronized (mRoomEventsLock) {
            eventsMap = mRoomEvents.get(roomId);
        }

        List<Event> eventsList;

        synchronized (mRoomEventsLock) {
            eventsList = new ArrayList<>(eventsMap.values());
        }

        int startIndex = 0;

        // try to reduce the number of stored messages
        // it does not make sense to keep the full history.

        // the method consists in saving messages until finding the oldest known token.
        // At initial sync, it is not saved so keep the whole history.
        // if the user back paginates, the token is stored in the event.
        // if some messages are received, the token is stored in the event.
        if (eventsList.size() > MAX_STORED_MESSAGES_COUNT) {
            // search backward the first known token
            for (startIndex = eventsList.size() - MAX_STORED_MESSAGES_COUNT; !eventsList.get(startIndex).hasToken() && (startIndex > 0); startIndex--)
                ;

            if (startIndex > 0) {
                Log.d(LOG_TAG, "## getSavedEveventsMap() : " + roomId + " reduce the number of messages " + eventsList.size()
                        + " -> " + (eventsList.size() - startIndex));
            }
        }

        LinkedHashMap<String, Event> savedEvents = new LinkedHashMap<>();

        for (int index = startIndex; index < eventsList.size(); index++) {
            Event event = eventsList.get(index);
            savedEvents.put(event.eventId, event);
        }

        return savedEvents;
    }

    private void saveRoomMessages(String roomId) {
        LinkedHashMap<String, Event> eventsHash;
        synchronized (mRoomEventsLock) {
            eventsHash = mRoomEvents.get(roomId);
        }

        String token = mRoomTokens.get(roomId);

        // the list exists ?
        if ((null != eventsHash) && (null != token)) {
            long t0 = System.currentTimeMillis();

            LinkedHashMap<String, Event> savedEventsMap = getSavedEventsMap(roomId);

            if (!writeObject("saveRoomsMessage " + roomId, new File(mGzStoreRoomsMessagesFolderFile, roomId), savedEventsMap)) {
                return;
            }

            if (!writeObject("saveRoomsMessage " + roomId, new File(mStoreRoomsTokensFolderFile, roomId), token)) {
                return;
            }

            Log.d(LOG_TAG, "saveRoomsMessage (" + roomId + ") : " + savedEventsMap.size() + " messages saved in " + (System.currentTimeMillis() - t0) + " ms");
        } else {
            deleteRoomMessagesFiles(roomId);
        }
    }

    /**
     * Flush updates rooms messages list files.
     */
    private void saveRoomsMessages() {
        // some updated rooms ?
        if ((mRoomsToCommitForMessages.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fRoomsToCommitForMessages = mRoomsToCommitForMessages;
            mRoomsToCommitForMessages = new HashSet<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mFileStoreHandler.post(new Runnable() {
                        public void run() {
                            if (!isKilled()) {
                                long start = System.currentTimeMillis();

                                for (String roomId : fRoomsToCommitForMessages) {
                                    saveRoomMessages(roomId);
                                }

                                Log.d(LOG_TAG, "saveRoomsMessages : " + fRoomsToCommitForMessages.size() + " rooms in "
                                        + (System.currentTimeMillis() - start) + " ms");
                            }
                        }
                    });
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    /**
     * Load room messages from the filesystem.
     *
     * @param roomId the room id.
     * @return true if succeed.
     */
    private boolean loadRoomMessages(final String roomId) {
        boolean succeeded = true;
        boolean shouldSave = false;
        LinkedHashMap<String, Event> events = null;

        File messagesListFile = new File(mGzStoreRoomsMessagesFolderFile, roomId);

        if (messagesListFile.exists()) {
            Object eventsAsVoid = readObject("events " + roomId, messagesListFile);

            if (null != eventsAsVoid) {
                try {
                    events = (LinkedHashMap<String, Event>) eventsAsVoid;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "loadRoomMessages " + roomId + "failed : " + e.getMessage(), e);
                    return false;
                }

                if (events.size() > (2 * MAX_STORED_MESSAGES_COUNT)) {
                    Log.d(LOG_TAG, "## loadRoomMessages() : the room " + roomId + " has " + events.size()
                            + " stored events : we need to find a way to reduce it.");
                }

                // finalizes the deserialization
                for (Event event : events.values()) {
                    // if a message was not sent, mark it as UNDELIVERED
                    if ((event.mSentState == Event.SentState.UNSENT)
                            || (event.mSentState == Event.SentState.SENDING)
                            || (event.mSentState == Event.SentState.WAITING_RETRY)
                            || (event.mSentState == Event.SentState.ENCRYPTING)) {
                        event.mSentState = Event.SentState.UNDELIVERED;
                        shouldSave = true;
                    }
                }
            } else {
                return false;
            }
        }

        // succeeds to extract the message list
        if (null != events) {
            // create the room object
            final Room room = new Room(getDataHandler(), this, roomId);
            // do not wait that the live state update
            room.setReadyState(true);
            storeRoom(room);

            mRoomEvents.put(roomId, events);
        }

        if (shouldSave) {
            saveRoomMessages(roomId);
        }

        return succeeded;
    }

    /**
     * Load the room token from the file system.
     *
     * @param roomId the room id.
     * @return true if it succeeds.
     */
    private boolean loadRoomToken(final String roomId) {
        boolean succeed = true;

        Room room = getRoom(roomId);

        // should always be true
        if (null != room) {
            String token = null;

            try {
                File messagesListFile = new File(mStoreRoomsTokensFolderFile, roomId);
                Object tokenAsVoid = readObject("loadRoomToken " + roomId, messagesListFile);

                if (null == tokenAsVoid) {
                    succeed = false;
                } else {
                    token = (String) tokenAsVoid;

                    // check if the oldest event has a token.
                    LinkedHashMap<String, Event> eventsHash = mRoomEvents.get(roomId);
                    if ((null != eventsHash) && (eventsHash.size() > 0)) {
                        Event event = eventsHash.values().iterator().next();

                        // the room history could have been reduced to save memory
                        // so, if the oldest messages has a token, use it instead of the stored token.
                        if (null != event.mToken) {
                            token = event.mToken;
                        }
                    }
                }
            } catch (Exception e) {
                succeed = false;
                Log.e(LOG_TAG, "loadRoomToken failed : " + e.toString(), e);
            }

            if (null != token) {
                mRoomTokens.put(roomId, token);
            } else {
                deleteRoom(roomId);
            }
        } else {
            try {
                File messagesListFile = new File(mStoreRoomsTokensFolderFile, roomId);
                messagesListFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "loadRoomToken failed with error " + e.getMessage(), e);
            }
        }

        return succeed;
    }

    /**
     * Load room messages from the filesystem.
     *
     * @return true if the operation succeeds.
     */
    private boolean loadRoomsMessages() {
        boolean succeed = true;

        try {
            // extract the messages list
            List<String> filenames = listFiles(mGzStoreRoomsMessagesFolderFile.list());

            long start = System.currentTimeMillis();

            for (String filename : filenames) {
                if (succeed) {
                    succeed &= loadRoomMessages(filename);
                }
            }

            if (succeed) {
                long delta = (System.currentTimeMillis() - start);
                Log.d(LOG_TAG, "loadRoomMessages : " + filenames.size() + " rooms in " + delta + " ms");
                mStoreStats.put("loadRoomMessages", delta);
            }

            // extract the tokens list
            filenames = listFiles(mStoreRoomsTokensFolderFile.list());

            start = System.currentTimeMillis();

            for (String filename : filenames) {
                if (succeed) {
                    succeed &= loadRoomToken(filename);
                }
            }

            if (succeed) {
                Log.d(LOG_TAG, "loadRoomToken : " + filenames.size() + " rooms in " + (System.currentTimeMillis() - start) + " ms");
            }

        } catch (Exception e) {
            succeed = false;
            Log.e(LOG_TAG, "loadRoomToken failed : " + e.getMessage(), e);
        }

        return succeed;
    }

    //================================================================================
    // Room states management
    //================================================================================

    // waiting that the rooms state events are loaded
    private Map<String, List<Event>> mPendingRoomStateEvents = new HashMap<>();

    @Override
    public void storeRoomStateEvent(final String roomId, final Event event) {
        /*boolean isAlreadyLoaded = true;

        synchronized (mRoomStateEventsByRoomId) {
            isAlreadyLoaded = mRoomStateEventsByRoomId.containsKey(roomId);
        }

        if (isAlreadyLoaded) {
            super.storeRoomStateEvent(roomId, event);
            mRoomsToCommitForStatesEvents.add(roomId);
            return;
        }

        boolean isRequestPending = false;

        synchronized (mPendingRoomStateEvents) {
            // a loading is already in progress
            if (mPendingRoomStateEvents.containsKey(roomId)) {
                mPendingRoomStateEvents.get(roomId).add(event);
                isRequestPending = true;
            }
        }

        if (isRequestPending) {
            return;
        }

        synchronized (mPendingRoomStateEvents) {
            List<Event> events = new ArrayList<Event>();
            events.add(event);
            mPendingRoomStateEvents.put(roomId, events);
        }

        getRoomStateEvents(roomId, new SimpleApiCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                List<Event> pendingEvents;

                synchronized (mPendingRoomStateEvents) {
                    pendingEvents = mPendingRoomStateEvents.get(roomId);
                    mPendingRoomStateEvents.remove(roomId);
                }

                // add them by now
                for (Event event : pendingEvents) {
                    storeRoomStateEvent(roomId, event);
                }
            }
        });*/
    }

    /**
     * Save the room state.
     *
     * @param roomId the room id.
     */
    private void saveRoomStateEvents(final String roomId) {
        /*Log.d(LOG_TAG, "++ saveRoomStateEvents " + roomId);
        
        File roomStateFile = new File(mGzStoreRoomsStateEventsFolderFile, roomId);
        Map<String, Event> eventsMap = mRoomStateEventsByRoomId.get(roomId);

        if (null != eventsMap) {
            List<Event> events = new ArrayList<>(eventsMap.values());

            long start1 = System.currentTimeMillis();
            writeObject("saveRoomStateEvents " + roomId, roomStateFile, events);
            Log.d(LOG_TAG, "saveRoomStateEvents " + roomId + " :" + events.size() + " events : " + (System.currentTimeMillis() - start1) + " ms");
        } else {
            Log.d(LOG_TAG, "-- saveRoomStateEvents " + roomId  + " : empty list");
        }*/
    }

    /**
     * Flush the room state events files.
     */
    private void saveRoomStatesEvents() {
        /*if ((mRoomsToCommitForStatesEvents.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fRoomsToCommitForStatesEvents = new HashSet<>(mRoomsToCommitForStatesEvents);
            mRoomsToCommitForStatesEvents = new HashSet<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mFileStoreHandler.post(new Runnable() {
                        public void run() {
                            if (!isKilled()) {
                                long start = System.currentTimeMillis();

                                for (String roomId : fRoomsToCommitForStatesEvents) {
                                    saveRoomStateEvents(roomId);
                                }

                                Log.d(LOG_TAG, "saveRoomStatesEvents : " + fRoomsToCommitForStatesEvents.size() + " rooms in "
                                 + (System.currentTimeMillis() - start) + " ms");
                            }
                        }
                    });
                }
            };

            Thread t = new Thread(r);
            t.start();
        }*/
    }

    @Override
    public void getRoomStateEvents(final String roomId, final ApiCallback<List<Event>> callback) {
        boolean isAlreadyLoaded = true;

        /*synchronized (mRoomStateEventsByRoomId) {
            isAlreadyLoaded = mRoomStateEventsByRoomId.containsKey(roomId);
        }*/

        if (isAlreadyLoaded) {
            super.getRoomStateEvents(roomId, callback);
            return;
        }

        /*Runnable r = new Runnable() {
            @Override
            public void run() {
                mFileStoreHandler.post(new Runnable() {
                    public void run() {
                        if (!isKilled()) {
                            File statesEventsFile = new File(mGzStoreRoomsStateEventsFolderFile, roomId);
                            Map<String, Event> eventsMap = new HashMap<>();
                            List<Event> eventsList = new ArrayList<>();

                            long start = System.currentTimeMillis();

                            if ((null != statesEventsFile) && statesEventsFile.exists()) {
                                try {
                                    Object eventsListAsVoid = readObject("getRoomStateEvents", statesEventsFile);

                                    if (null != eventsListAsVoid) {
                                        List<Event> events = (List<Event>) eventsListAsVoid;

                                        for (Event event : events) {
                                            eventsMap.put(event.stateKey, event);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "getRoomStateEvents failed : " + e.getMessage(), e);
                                }
                            }

                            synchronized (mRoomStateEventsByRoomId) {
                                mRoomStateEventsByRoomId.put(roomId, eventsMap);
                            }

                            Log.d(LOG_TAG, "getRoomStateEvents : retrieve " + eventsList.size() + " events in " + (System.currentTimeMillis() - start) + " ms");
                            callback.onSuccess(eventsList);
                        }
                    }
                });
            }
        };

        Thread t = new Thread(r);
        t.start();*/
    }

    /**
     * Delete the room state file.
     *
     * @param roomId the room id.
     */
    private void deleteRoomStateFile(String roomId) {
        // states list
        File statesFile = new File(mGzStoreRoomsStateFolderFile, roomId);

        if (statesFile.exists()) {
            try {
                statesFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteRoomStateFile failed with error " + e.getMessage(), e);
            }
        }

        File statesEventsFile = new File(mGzStoreRoomsStateEventsFolderFile, roomId);

        if (statesEventsFile.exists()) {
            try {
                statesEventsFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteRoomStateFile failed with error " + e.getMessage(), e);
            }
        }
    }

    /**
     * Save the room state.
     *
     * @param roomId the room id.
     */
    private void saveRoomState(final String roomId) {
        Log.d(LOG_TAG, "++ saveRoomsState " + roomId);

        File roomStateFile = new File(mGzStoreRoomsStateFolderFile, roomId);
        Room room = mRooms.get(roomId);

        if (null != room) {
            long start1 = System.currentTimeMillis();
            writeObject("saveRoomsState " + roomId, roomStateFile, room.getState());
            Log.d(LOG_TAG, "saveRoomsState " + room.getNumberOfMembers() + " members : " + (System.currentTimeMillis() - start1) + " ms");
        } else {
            Log.d(LOG_TAG, "saveRoomsState : delete the room state");
            deleteRoomStateFile(roomId);
        }

        Log.d(LOG_TAG, "-- saveRoomsState " + roomId);
    }

    /**
     * Flush the room state files.
     */
    private void saveRoomStates() {
        if ((mRoomsToCommitForStates.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fRoomsToCommitForStates = mRoomsToCommitForStates;
            mRoomsToCommitForStates = new HashSet<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mFileStoreHandler.post(new Runnable() {
                        public void run() {
                            if (!isKilled()) {
                                long start = System.currentTimeMillis();

                                for (String roomId : fRoomsToCommitForStates) {
                                    saveRoomState(roomId);
                                }

                                Log.d(LOG_TAG, "saveRoomsState : " + fRoomsToCommitForStates.size() + " rooms in "
                                        + (System.currentTimeMillis() - start) + " ms");
                            }
                        }
                    });
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    /**
     * Load a room state from the file system.
     *
     * @param roomId the room id.
     * @return true if the operation succeeds.
     */
    private boolean loadRoomState(final String roomId) {
        boolean succeed = true;

        Room room = getRoom(roomId);

        // should always be true
        if (null != room) {
            RoomState liveState = null;

            try {
                // the room state is not zipped
                File roomStateFile = new File(mGzStoreRoomsStateFolderFile, roomId);

                // new format
                if (roomStateFile.exists()) {
                    Object roomStateAsObject = readObject("loadRoomState " + roomId, roomStateFile);

                    if (null == roomStateAsObject) {
                        succeed = false;
                    } else {
                        liveState = (RoomState) roomStateAsObject;
                    }
                }
            } catch (Exception e) {
                succeed = false;
                Log.e(LOG_TAG, "loadRoomState failed : " + e.getMessage(), e);
            }

            if (null != liveState) {
                room.getTimeline().setState(liveState);
            } else {
                deleteRoom(roomId);
            }
        } else {
            try {
                File messagesListFile = new File(mGzStoreRoomsStateFolderFile, roomId);
                messagesListFile.delete();

            } catch (Exception e) {
                Log.e(LOG_TAG, "loadRoomState failed to delete a file : " + e.getMessage(), e);
            }
        }

        return succeed;
    }

    /**
     * Load room state from the file system.
     *
     * @return true if the operation succeeds.
     */
    private boolean loadRoomsState() {
        boolean succeed = true;

        try {
            long start = System.currentTimeMillis();

            List<String> filenames = listFiles(mGzStoreRoomsStateFolderFile.list());

            for (String filename : filenames) {
                if (succeed) {
                    succeed &= loadRoomState(filename);
                }
            }

            long delta = (System.currentTimeMillis() - start);
            Log.d(LOG_TAG, "loadRoomsState " + filenames.size() + " rooms in " + delta + " ms");
            mStoreStats.put("loadRoomsState", delta);

        } catch (Exception e) {
            succeed = false;
            Log.e(LOG_TAG, "loadRoomsState failed : " + e.getMessage(), e);
        }

        return succeed;
    }

    //================================================================================
    // AccountData management
    //================================================================================

    /**
     * Delete the room account data file.
     *
     * @param roomId the room id.
     */
    private void deleteRoomAccountDataFile(String roomId) {
        File file = new File(mStoreRoomsAccountDataFolderFile, roomId);

        // remove the files
        if (file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteRoomAccountDataFile failed : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Flush the pending account data.
     */
    private void saveRoomsAccountData() {
        if ((mRoomsToCommitForAccountData.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fRoomsToCommitForAccountData = mRoomsToCommitForAccountData;
            mRoomsToCommitForAccountData = new HashSet<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mFileStoreHandler.post(new Runnable() {
                        public void run() {
                            if (!isKilled()) {
                                long start = System.currentTimeMillis();

                                for (String roomId : fRoomsToCommitForAccountData) {
                                    RoomAccountData accountData = mRoomAccountData.get(roomId);

                                    if (null != accountData) {
                                        writeObject("saveRoomsAccountData " + roomId, new File(mStoreRoomsAccountDataFolderFile, roomId), accountData);
                                    } else {
                                        deleteRoomAccountDataFile(roomId);
                                    }
                                }

                                Log.d(LOG_TAG, "saveSummaries : " + fRoomsToCommitForAccountData.size() + " account data in "
                                        + (System.currentTimeMillis() - start) + " ms");
                            }
                        }
                    });
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    /***
     * Load the account Data of a dedicated room.
     * @param roomId the room Id
     * @return true if the operation succeeds.
     */
    private boolean loadRoomAccountData(final String roomId) {
        boolean succeeded = true;
        RoomAccountData roomAccountData = null;

        try {
            File accountDataFile = new File(mStoreRoomsAccountDataFolderFile, roomId);

            if (accountDataFile.exists()) {
                Object accountAsVoid = readObject("loadRoomAccountData " + roomId, accountDataFile);

                if (null == accountAsVoid) {
                    Log.e(LOG_TAG, "loadRoomAccountData failed");
                    return false;
                }

                roomAccountData = (RoomAccountData) accountAsVoid;
            }
        } catch (Exception e) {
            succeeded = false;
            Log.e(LOG_TAG, "loadRoomAccountData failed : " + e.toString(), e);
        }

        // succeeds to extract the message list
        if (null != roomAccountData) {
            Room room = getRoom(roomId);

            if (null != room) {
                room.setAccountData(roomAccountData);
            }
        }

        return succeeded;
    }

    /**
     * Load room accountData from the filesystem.
     *
     * @return true if the operation succeeds.
     */
    private boolean loadRoomsAccountData() {
        boolean succeed = true;

        try {
            // extract the messages list
            List<String> filenames = listFiles(mStoreRoomsAccountDataFolderFile.list());

            long start = System.currentTimeMillis();

            for (String filename : filenames) {
                succeed &= loadRoomAccountData(filename);
            }

            if (succeed) {
                Log.d(LOG_TAG, "loadRoomsAccountData : " + filenames.size() + " rooms in " + (System.currentTimeMillis() - start) + " ms");
            }
        } catch (Exception e) {
            succeed = false;
            Log.e(LOG_TAG, "loadRoomsAccountData failed : " + e.getMessage(), e);
        }

        return succeed;
    }

    @Override
    public void storeAccountData(String roomId, RoomAccountData accountData) {
        super.storeAccountData(roomId, accountData);

        if (null != roomId) {
            Room room = mRooms.get(roomId);

            // sanity checks
            if ((room != null) && (null != accountData)) {
                mRoomsToCommitForAccountData.add(roomId);
            }
        }
    }

    //================================================================================
    // Summary management
    //================================================================================

    /**
     * Delete the room summary file.
     *
     * @param roomId the room id.
     */
    private void deleteRoomSummaryFile(String roomId) {
        // states list
        File statesFile = new File(mStoreRoomsSummaryFolderFile, roomId);

        // remove the files
        if (statesFile.exists()) {
            try {
                statesFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteRoomSummaryFile failed : " + e.getMessage(), e);
            }
        }
    }

    /**
     * Flush the pending summaries.
     */
    private void saveSummaries() {
        if ((mRoomsToCommitForSummaries.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fRoomsToCommitForSummaries = mRoomsToCommitForSummaries;
            mRoomsToCommitForSummaries = new HashSet<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mFileStoreHandler.post(new Runnable() {
                        public void run() {
                            if (!isKilled()) {
                                long start = System.currentTimeMillis();

                                for (String roomId : fRoomsToCommitForSummaries) {
                                    try {
                                        File roomSummaryFile = new File(mStoreRoomsSummaryFolderFile, roomId);
                                        RoomSummary roomSummary = mRoomSummaries.get(roomId);

                                        if (null != roomSummary) {
                                            writeObject("saveSummaries " + roomId, roomSummaryFile, roomSummary);
                                        } else {
                                            deleteRoomSummaryFile(roomId);
                                        }
                                    } catch (OutOfMemoryError oom) {
                                        dispatchOOM(oom);
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "saveSummaries failed : " + e.getMessage(), e);
                                        // Toast.makeText(mContext, "saveSummaries failed " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }

                                Log.d(LOG_TAG, "saveSummaries : " + fRoomsToCommitForSummaries.size() + " summaries in "
                                        + (System.currentTimeMillis() - start) + " ms");
                            }
                        }
                    });
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    /**
     * Load the room summary from the files system.
     *
     * @param roomId the room id.
     * @return true if the operation succeeds;
     */
    private boolean loadSummary(final String roomId) {
        boolean succeed = true;

        // do not check if the room exists here.
        // if the user is invited to a room, the room object is not created until it is joined.
        RoomSummary summary = null;

        try {
            File messagesListFile = new File(mStoreRoomsSummaryFolderFile, roomId);
            Object summaryAsVoid = readObject("loadSummary " + roomId, messagesListFile);

            if (null == summaryAsVoid) {
                Log.e(LOG_TAG, "loadSummary failed");
                return false;
            }

            summary = (RoomSummary) summaryAsVoid;
        } catch (Exception e) {
            succeed = false;
            Log.e(LOG_TAG, "loadSummary failed : " + e.getMessage(), e);
        }

        if (null != summary) {
            //summary.getLatestReceivedEvent().finalizeDeserialization();

            Room room = getRoom(summary.getRoomId());

            // the room state is not saved in the summary.
            // it is restored from the room
            if (null != room) {
                summary.setLatestRoomState(room.getState());
            }

            mRoomSummaries.put(roomId, summary);
        }

        return succeed;
    }

    /**
     * Load room summaries from the file system.
     *
     * @return true if the operation succeeds.
     */
    private boolean loadSummaries() {
        boolean succeed = true;
        try {
            // extract the room states
            List<String> filenames = listFiles(mStoreRoomsSummaryFolderFile.list());

            long start = System.currentTimeMillis();

            for (String filename : filenames) {
                succeed &= loadSummary(filename);
            }

            long delta = (System.currentTimeMillis() - start);
            Log.d(LOG_TAG, "loadSummaries " + filenames.size() + " rooms in " + delta + " ms");
            mStoreStats.put("loadSummaries", delta);
        } catch (Exception e) {
            succeed = false;
            Log.e(LOG_TAG, "loadSummaries failed : " + e.getMessage(), e);
        }

        return succeed;
    }

    //================================================================================
    // Metadata management
    //================================================================================

    /**
     * Load the metadata info from the file system.
     */
    private void loadMetaData() {
        long start = System.currentTimeMillis();

        // init members
        mEventStreamToken = null;
        mMetadata = null;

        File metaDataFile = new File(mStoreFolderFile, MXFILE_STORE_METADATA_FILE_NAME);

        if (metaDataFile.exists()) {
            Object metadataAsVoid = readObject("loadMetaData", metaDataFile);

            if (null != metadataAsVoid) {
                try {
                    mMetadata = (MXFileStoreMetaData) metadataAsVoid;

                    // remove pending \n
                    if (null != mMetadata.mUserDisplayName) {
                        mMetadata.mUserDisplayName.trim();
                    }

                    // extract the latest event stream token
                    mEventStreamToken = mMetadata.mEventStreamToken;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## loadMetaData() : is corrupted", e);
                    return;
                }
            }
        }

        Log.d(LOG_TAG, "loadMetaData : " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * flush the metadata info from the file system.
     */
    private void saveMetaData() {
        if ((mMetaDataHasChanged) && (null != mFileStoreHandler) && (null != mMetadata)) {
            mMetaDataHasChanged = false;

            final MXFileStoreMetaData fMetadata = mMetadata.deepCopy();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mFileStoreHandler.post(new Runnable() {
                        public void run() {
                            if (!mIsKilled) {
                                // save the metadata only when there is a current valid stream token
                                // avoid saving the metadata if the store has been cleared
                                if (null != mMetadata.mEventStreamToken) {
                                    long start = System.currentTimeMillis();
                                    writeObject("saveMetaData", new File(mStoreFolderFile, MXFILE_STORE_METADATA_FILE_NAME), fMetadata);
                                    Log.d(LOG_TAG, "saveMetaData : " + (System.currentTimeMillis() - start) + " ms");
                                } else {
                                    Log.e(LOG_TAG, "## saveMetaData() : cancelled because mEventStreamToken is null");
                                }
                            }
                        }
                    });
                }
            };

            Thread t = new Thread(r);
            t.start();
        }
    }

    //================================================================================
    // Event receipts management
    //================================================================================

    @Override
    public List<ReceiptData> getEventReceipts(String roomId, String eventId, boolean excludeSelf, boolean sort) {
        synchronized (mRoomReceiptsToLoad) {
            int pos = mRoomReceiptsToLoad.indexOf(roomId);

            // the user requires the receipts asap
            if (pos >= 2) {
                mRoomReceiptsToLoad.remove(roomId);
                // index 0 is the current managed one
                mRoomReceiptsToLoad.add(1, roomId);
            }
        }

        return super.getEventReceipts(roomId, eventId, excludeSelf, sort);
    }

    /**
     * Store the receipt for an user in a room
     *
     * @param receipt The event
     * @param roomId  The roomId
     * @return true if the receipt has been stored
     */
    @Override
    public boolean storeReceipt(ReceiptData receipt, String roomId) {
        boolean res = super.storeReceipt(receipt, roomId);

        if (res) {
            synchronized (this) {
                mRoomsToCommitForReceipts.add(roomId);
            }
        }

        return res;
    }

    /***
     * Load the events receipts.
     * @param roomId the room Id
     * @return true if the operation succeeds.
     */
    private boolean loadReceipts(String roomId) {
        Map<String, ReceiptData> receiptsMap = null;
        File file = new File(mStoreRoomsMessagesReceiptsFolderFile, roomId);

        if (file.exists()) {
            Object receiptsAsVoid = readObject("loadReceipts " + roomId, file);

            if (null != receiptsAsVoid) {
                try {
                    List<ReceiptData> receipts = (List<ReceiptData>) receiptsAsVoid;

                    receiptsMap = new HashMap<>();

                    for (ReceiptData r : receipts) {
                        receiptsMap.put(r.userId, r);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "loadReceipts failed : " + e.getMessage(), e);
                    return false;
                }
            } else {
                return false;
            }
        }

        if (null != receiptsMap) {
            Map<String, ReceiptData> currentReceiptMap;

            synchronized (mReceiptsByRoomIdLock) {
                currentReceiptMap = mReceiptsByRoomId.get(roomId);
                mReceiptsByRoomId.put(roomId, receiptsMap);
            }

            // merge the current read receipts
            if (null != currentReceiptMap) {
                Collection<ReceiptData> receipts = currentReceiptMap.values();

                for (ReceiptData receipt : receipts) {
                    storeReceipt(receipt, roomId);
                }
            }

            dispatchOnReadReceiptsLoaded(roomId);
        }

        return true;
    }

    /**
     * Load event receipts from the file system.
     *
     * @return true if the operation succeeds.
     */
    private boolean loadReceipts() {
        boolean succeed = true;
        try {
            int count = mRoomReceiptsToLoad.size();
            long start = System.currentTimeMillis();

            while (mRoomReceiptsToLoad.size() > 0) {
                String roomId;
                synchronized (mRoomReceiptsToLoad) {
                    roomId = mRoomReceiptsToLoad.get(0);
                }

                loadReceipts(roomId);

                synchronized (mRoomReceiptsToLoad) {
                    mRoomReceiptsToLoad.remove(0);
                }
            }

            saveReceipts();

            long delta = (System.currentTimeMillis() - start);
            Log.d(LOG_TAG, "loadReceipts " + count + " rooms in " + delta + " ms");
            mStoreStats.put("loadReceipts", delta);
        } catch (Exception e) {
            succeed = false;
            //Toast.makeText(mContext, "loadReceipts failed" + e, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "loadReceipts failed : " + e.getMessage(), e);
        }

        synchronized (this) {
            mAreReceiptsReady = true;
        }

        return succeed;
    }

    /**
     * Flush the events receipts
     *
     * @param roomId the roomId.
     */
    private void saveReceipts(final String roomId) {
        synchronized (mRoomReceiptsToLoad) {
            // please wait
            if (mRoomReceiptsToLoad.contains(roomId)) {
                return;
            }
        }

        final List<ReceiptData> receipts;

        synchronized (mReceiptsByRoomIdLock) {
            if (mReceiptsByRoomId.containsKey(roomId)) {
                receipts = new ArrayList<>(mReceiptsByRoomId.get(roomId).values());
            } else {
                receipts = null;
            }
        }

        // sanity check
        if (null == receipts) {
            return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                mFileStoreHandler.post(new Runnable() {
                    public void run() {
                        if (!mIsKilled) {
                            long start = System.currentTimeMillis();
                            writeObject("saveReceipts " + roomId, new File(mStoreRoomsMessagesReceiptsFolderFile, roomId), receipts);
                            Log.d(LOG_TAG, "saveReceipts : roomId " + roomId + " eventId : " + (System.currentTimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    /**
     * Save the events receipts.
     */
    private void saveReceipts() {
        synchronized (this) {
            Set<String> roomsToCommit = mRoomsToCommitForReceipts;

            for (String roomId : roomsToCommit) {
                saveReceipts(roomId);
            }

            mRoomsToCommitForReceipts.clear();
        }
    }

    /**
     * Delete the room receipts
     *
     * @param roomId the room id.
     */
    private void deleteRoomReceiptsFile(String roomId) {
        File receiptsFile = new File(mStoreRoomsMessagesReceiptsFolderFile, roomId);

        // remove the files
        if (receiptsFile.exists()) {
            try {
                receiptsFile.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "deleteReceiptsFile - failed " + e.getMessage(), e);
            }
        }
    }

    //================================================================================
    // read/write methods
    //================================================================================

    /**
     * Write an object in a dedicated file.
     *
     * @param description the operation description
     * @param file        the file
     * @param object      the object to save
     * @return true if the operation succeeds
     */
    private boolean writeObject(String description, File file, Object object) {
        String parent = file.getParent();
        String name = file.getName();

        File tmpFile = new File(parent, name + ".tmp");

        if (tmpFile.exists()) {
            tmpFile.delete();
        }

        if (file.exists()) {
            file.renameTo(tmpFile);
        }

        boolean succeed = false;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStream cos;
            if (mEnableFileEncryption) {
                cos = CompatUtil.createCipherOutputStream(fos, mContext);
            } else {
                cos = fos;
            }
            GZIPOutputStream gz = CompatUtil.createGzipOutputStream(cos);
            ObjectOutputStream out = new ObjectOutputStream(gz);

            out.writeObject(object);
            out.flush();
            out.close();

            succeed = true;
        } catch (OutOfMemoryError oom) {
            dispatchOOM(oom);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## writeObject()  " + description + " : failed " + e.getMessage(), e);
        }

        if (succeed) {
            tmpFile.delete();
        } else {
            tmpFile.renameTo(file);
        }

        return succeed;
    }

    /**
     * Read an object from a dedicated file
     *
     * @param description the operation description
     * @param file        the file
     * @return the read object if it can be retrieved
     */
    private Object readObject(String description, File file) {
        String parent = file.getParent();
        String name = file.getName();

        File tmpFile = new File(parent, name + ".tmp");

        if (tmpFile.exists()) {
            Log.e(LOG_TAG, "## readObject : rescue from a tmp file " + tmpFile.getName());
            file = tmpFile;
        }

        Object object = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStream cis;
            if (mEnableFileEncryption) {
                cis = CompatUtil.createCipherInputStream(fis, mContext);

                if (cis == null) {
                    // fallback to unencrypted stream for backward compatibility
                    Log.i(LOG_TAG, "## readObject() : failed to read encrypted, fallback to unencrypted read");
                    fis.close();
                    cis = new FileInputStream(file);
                }
            } else {
                cis = fis;
            }

            GZIPInputStream gz = new GZIPInputStream(cis);
            ObjectInputStream ois = new ObjectInputStream(gz);
            object = ois.readObject();
            ois.close();
        } catch (OutOfMemoryError oom) {
            dispatchOOM(oom);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## readObject()  " + description + " : failed " + e.getMessage(), e);
        }
        return object;
    }


    /**
     * Remove the tmp files from a filename list
     *
     * @param names the names list
     * @return the filtered list
     */
    private static List<String> listFiles(String[] names) {
        List<String> filteredFilenames = new ArrayList<>();
        List<String> tmpFilenames = new ArrayList<>();

        // sanity checks
        // it has been reported by GA
        if (null != names) {
            for (int i = 0; i < names.length; i++) {
                String name = names[i];

                if (!name.endsWith(".tmp")) {
                    filteredFilenames.add(name);
                } else {
                    tmpFilenames.add(name.substring(0, name.length() - ".tmp".length()));
                }
            }

            // check if the tmp file is not alone i.e the matched file was not saved (app crash...)
            for (String tmpFileName : tmpFilenames) {
                if (!filteredFilenames.contains(tmpFileName)) {
                    Log.e(LOG_TAG, "## listFiles() : " + tmpFileName + " does not exist but a tmp file has been retrieved");
                    filteredFilenames.add(tmpFileName);
                }
            }
        }

        return filteredFilenames;
    }

    /**
     * Start a runnable from the store thread
     *
     * @param runnable the runnable to call
     */
    public void post(Runnable runnable) {
        if (null != mFileStoreHandler) {
            mFileStoreHandler.post(runnable);
        } else {
            super.post(runnable);
        }
    }

    //================================================================================
    // groups management
    //================================================================================

    /**
     * Store a group
     *
     * @param group the group to store
     */
    @Override
    public void storeGroup(Group group) {
        super.storeGroup(group);
        if ((null != group) && !TextUtils.isEmpty(group.getGroupId())) {
            mGroupsToCommit.add(group.getGroupId());
        }
    }

    /**
     * Flush a group
     *
     * @param group the group to store
     */
    @Override
    public void flushGroup(Group group) {
        super.flushGroup(group);
        if ((null != group) && !TextUtils.isEmpty(group.getGroupId())) {
            mGroupsToCommit.add(group.getGroupId());
            saveGroups();
        }
    }

    /**
     * Delete a group
     *
     * @param groupId the groupId to delete
     */
    @Override
    public void deleteGroup(String groupId) {
        super.deleteGroup(groupId);
        if (!TextUtils.isEmpty(groupId)) {
            mGroupsToCommit.add(groupId);
        }
    }

    /**
     * Flush groups list
     */
    private void saveGroups() {
        // some updated rooms ?
        if ((mGroupsToCommit.size() > 0) && (null != mFileStoreHandler)) {
            // get the list
            final Set<String> fGroupIds = mGroupsToCommit;
            mGroupsToCommit = new HashSet<>();

            try {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        mFileStoreHandler.post(new Runnable() {
                            public void run() {
                                if (!isKilled()) {
                                    Log.d(LOG_TAG, "saveGroups " + fGroupIds.size() + " groups");

                                    long start = System.currentTimeMillis();

                                    for (String groupId : fGroupIds) {
                                        Group group;

                                        synchronized (mGroups) {
                                            group = mGroups.get(groupId);
                                        }

                                        if (null != group) {
                                            writeObject("saveGroup " + groupId, new File(mStoreGroupsFolderFile, groupId), group);
                                        } else {
                                            File tokenFile = new File(mStoreGroupsFolderFile, groupId);

                                            if (tokenFile.exists()) {
                                                tokenFile.delete();
                                            }
                                        }
                                    }

                                    Log.d(LOG_TAG, "saveGroups done in " + (System.currentTimeMillis() - start) + " ms");
                                }
                            }
                        });
                    }
                };

                Thread t = new Thread(r);
                t.start();
            } catch (OutOfMemoryError oom) {
                Log.e(LOG_TAG, "saveGroups : failed" + oom.getMessage(), oom);
            }
        }
    }

    /**
     * Load groups from the filesystem.
     *
     * @return true if the operation succeeds.
     */
    private boolean loadGroups() {
        boolean succeed = true;

        try {
            // extract the messages list
            List<String> filenames = listFiles(mStoreGroupsFolderFile.list());

            long start = System.currentTimeMillis();

            for (String filename : filenames) {
                File groupFile = new File(mStoreGroupsFolderFile, filename);

                if (groupFile.exists()) {
                    Object groupAsVoid = readObject("loadGroups " + filename, groupFile);

                    if ((null != groupAsVoid) && (groupAsVoid instanceof Group)) {
                        Group group = (Group) groupAsVoid;
                        mGroups.put(group.getGroupId(), group);
                    } else {
                        succeed = false;
                        break;
                    }
                }
            }

            if (succeed) {
                long delta = (System.currentTimeMillis() - start);
                Log.d(LOG_TAG, "loadGroups : " + filenames.size() + " groups in " + delta + " ms");
                mStoreStats.put("loadGroups", delta);
            }

        } catch (Exception e) {
            succeed = false;
            Log.e(LOG_TAG, "loadGroups failed : " + e.getMessage(), e);
        }

        return succeed;
    }

    @Override
    public void setURLPreviewEnabled(boolean value) {
        super.setURLPreviewEnabled(value);
        mMetaDataHasChanged = true;
    }

    @Override
    public void setRoomsWithoutURLPreview(Set<String> roomIds) {
        super.setRoomsWithoutURLPreview(roomIds);
        mMetaDataHasChanged = true;
    }

    @Override
    public void setUserWidgets(Map<String, Object> contentDict) {
        super.setUserWidgets(contentDict);
        mMetaDataHasChanged = true;
    }

    @Override
    public void addFilter(String jsonFilter, String filterId) {
        super.addFilter(jsonFilter, filterId);
        mMetaDataHasChanged = true;
    }

    @Override
    public void setAntivirusServerPublicKey(@Nullable String key) {
        super.setAntivirusServerPublicKey(key);
        mMetaDataHasChanged = true;
    }
}