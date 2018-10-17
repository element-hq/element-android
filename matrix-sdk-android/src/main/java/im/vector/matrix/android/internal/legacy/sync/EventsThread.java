/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.sync;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import im.vector.matrix.android.internal.legacy.data.metrics.MetricsListener;
import im.vector.matrix.android.internal.legacy.listeners.IMXNetworkEventListener;
import im.vector.matrix.android.internal.legacy.network.NetworkConnectivityReceiver;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiFailureCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.client.EventsRestClient;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomsSyncResponse;
import im.vector.matrix.android.internal.legacy.rest.model.sync.SyncResponse;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


/**
 * Thread that continually watches the event stream and sends events to its listener.
 */
public class EventsThread extends Thread {
    private static final String LOG_TAG = EventsThread.class.getSimpleName();

    private static final int RETRY_WAIT_TIME_MS = 10000;

    private static final int DEFAULT_SERVER_TIMEOUT_MS = 30000;
    private static final int DEFAULT_CLIENT_TIMEOUT_MS = 120000;

    private EventsRestClient mEventsRestClient;
    private EventsThreadListener mListener;
    private String mCurrentToken;

    private MetricsListener mMetricsListener;

    private boolean mPaused = true;
    private boolean mIsNetworkSuspended = false;
    private boolean mIsCatchingUp = false;
    private boolean mIsOnline = false;

    private boolean mKilling = false;

    private int mDefaultServerTimeoutms = DEFAULT_SERVER_TIMEOUT_MS;
    private int mNextServerTimeoutms = DEFAULT_SERVER_TIMEOUT_MS;

    // add a delay between two sync requests
    private final Context mContext;
    private int mRequestDelayMs = 0;
    private final AlarmManager mAlarmManager;
    private PowerManager mPowerManager;
    private PendingIntent mPendingDelayedIntent;
    private static final Map<String, EventsThread> mSyncObjectByInstance = new HashMap<>();

    // avoid sync on "this" because it might differ if there is a timer.
    private final Object mSyncObject = new Object();

    // Custom Retrofit error callback that will convert Retrofit errors into our own error callback
    private ApiFailureCallback mFailureCallback;

    // avoid restarting the listener if there is no network.
    // wait that there is an available network.
    private NetworkConnectivityReceiver mNetworkConnectivityReceiver;
    private boolean mbIsConnected = true;

    // use dedicated filter when enable
    private String mFilterOrFilterId;

    private final IMXNetworkEventListener mNetworkListener = new IMXNetworkEventListener() {
        @Override
        public void onNetworkConnectionUpdate(boolean isConnected) {
            Log.d(LOG_TAG, "onNetworkConnectionUpdate : before " + mbIsConnected + " now " + isConnected);

            synchronized (mSyncObject) {
                mbIsConnected = isConnected;
            }

            // the thread has been suspended and there is an available network
            if (isConnected && !mKilling) {
                Log.d(LOG_TAG, "onNetworkConnectionUpdate : call onNetworkAvailable");
                onNetworkAvailable();
            }
        }
    };

    /**
     * Default constructor.
     *
     * @param context      the context
     * @param apiClient    API client to make the events API calls
     * @param listener     a listener to inform
     * @param initialToken the sync initial token.
     */
    public EventsThread(Context context, EventsRestClient apiClient, EventsThreadListener listener, String initialToken) {
        super("Events thread");
        mContext = context;
        mEventsRestClient = apiClient;
        mListener = listener;
        mCurrentToken = initialToken;
        mSyncObjectByInstance.put(toString(), this);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    /**
     * Update the metrics listener mode
     *
     * @param metricsListener the metrics listener
     */

    public void setMetricsListener(MetricsListener metricsListener) {
        this.mMetricsListener = metricsListener;
    }

    /**
     * @return the current sync token
     */
    public String getCurrentSyncToken() {
        return mCurrentToken;
    }

    /**
     * Set filterOrFilterId used for /sync requests
     *
     * @param filterOrFilterId
     */
    public void setFilterOrFilterId(String filterOrFilterId) {
        mFilterOrFilterId = filterOrFilterId;
    }

    /**
     * Update the long poll timeout.
     *
     * @param ms the timeout in ms
     */
    public void setServerLongPollTimeout(int ms) {
        mDefaultServerTimeoutms = Math.max(ms, DEFAULT_SERVER_TIMEOUT_MS);
        Log.d(LOG_TAG, "setServerLongPollTimeout : " + mDefaultServerTimeoutms);

    }

    /**
     * @return the long poll timeout
     */
    public int getServerLongPollTimeout() {
        return mDefaultServerTimeoutms;
    }

    /**
     * Set a delay between two sync requests.
     *
     * @param ms the delay in ms
     */
    public void setSyncDelay(int ms) {
        mRequestDelayMs = Math.max(0, ms);

        Log.d(LOG_TAG, "## setSyncDelay() : " + mRequestDelayMs + " with state " + getState());

        if (State.WAITING == getState() && (!mPaused || (0 == mRequestDelayMs) && mIsCatchingUp)) {
            if (!mPaused) {
                Log.d(LOG_TAG, "## setSyncDelay() : resume the application");
            }

            if ((0 == mRequestDelayMs) && mIsCatchingUp) {
                Log.d(LOG_TAG, "## setSyncDelay() : cancel catchup");
                mIsCatchingUp = false;
            }

            // and sync asap
            synchronized (mSyncObject) {
                mSyncObject.notify();
            }
        }
    }

    /**
     * @return the delay between two sync requests.
     */
    public int getSyncDelay() {
        return mRequestDelayMs;
    }

    /**
     * Set the network connectivity listener.
     * It is used to avoid restarting the events threads each 10 seconds when there is no available network.
     *
     * @param networkConnectivityReceiver the network receiver
     */
    public void setNetworkConnectivityReceiver(NetworkConnectivityReceiver networkConnectivityReceiver) {
        mNetworkConnectivityReceiver = networkConnectivityReceiver;
    }

    /**
     * Set the failure callback.
     *
     * @param failureCallback the failure callback.
     */
    public void setFailureCallback(ApiFailureCallback failureCallback) {
        mFailureCallback = failureCallback;
    }

    /**
     * Pause the thread. It will resume where it left off when pickUp()d.
     */
    public void pause() {
        Log.d(LOG_TAG, "pause()");
        mPaused = true;
        mIsCatchingUp = false;
    }

    /**
     * A network connection has been retrieved.
     */
    private void onNetworkAvailable() {
        Log.d(LOG_TAG, "onNetWorkAvailable()");
        if (mIsNetworkSuspended) {
            mIsNetworkSuspended = false;

            if (mPaused) {
                Log.d(LOG_TAG, "the event thread is still suspended");
            } else {
                Log.d(LOG_TAG, "Resume the thread");
                // cancel any catchup process.
                mIsCatchingUp = false;

                synchronized (mSyncObject) {
                    mSyncObject.notify();
                }
            }
        } else {
            Log.d(LOG_TAG, "onNetWorkAvailable() : nothing to do");
        }
    }

    /**
     * Unpause the thread if it had previously been paused. If not, this does nothing.
     */
    public void unpause() {
        Log.d(LOG_TAG, "## pickUp() : thread state " + getState());

        if (State.WAITING == getState()) {
            Log.d(LOG_TAG, "## pickUp() : the thread was paused so resume it.");

            mPaused = false;
            synchronized (mSyncObject) {
                mSyncObject.notify();
            }
        }

        // cancel any catchup process.
        mIsCatchingUp = false;
    }

    /**
     * Catchup until some events are retrieved.
     */
    public void catchup() {
        Log.d(LOG_TAG, "## catchup() : thread state " + getState());

        if (State.WAITING == getState()) {
            Log.d(LOG_TAG, "## catchup() : the thread was paused so wake it up");

            mPaused = false;
            synchronized (mSyncObject) {
                mSyncObject.notify();
            }
        }

        mIsCatchingUp = true;
    }

    /**
     * Allow the thread to finish its current processing, then permanently stop.
     */
    public void kill() {
        Log.d(LOG_TAG, "killing ...");

        mKilling = true;

        if (mPaused) {
            Log.d(LOG_TAG, "killing : the thread was pause so wake it up");

            mPaused = false;
            synchronized (mSyncObject) {
                mSyncObject.notify();
            }

            Log.d(LOG_TAG, "Resume the thread to kill it.");
        }
    }

    /**
     * Cancel the killing process
     */
    public void cancelKill() {
        if (mKilling) {
            Log.d(LOG_TAG, "## cancelKill() : Cancel the pending kill");
            mKilling = false;
        } else {
            Log.d(LOG_TAG, "## cancelKill() : Nothing to d");
        }
    }

    /**
     * Update the online status
     *
     * @param isOnline true if the client must be seen as online
     */
    public void setIsOnline(boolean isOnline) {
        Log.d(LOG_TAG, "setIsOnline to " + isOnline);
        mIsOnline = isOnline;
    }

    /**
     * Tells if the presence is online.
     *
     * @return true if the user is seen as online.
     */
    public boolean isOnline() {
        return mIsOnline;
    }

    @Override
    public void run() {
        try {
            Looper.prepare();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## run() : prepare failed " + e.getMessage(), e);
        }
        startSync();
    }

    /**
     * Tells if a sync request contains some changed devices.
     *
     * @param syncResponse the sync response
     * @return true if the response contains some changed devices.
     */
    private static boolean hasDevicesChanged(SyncResponse syncResponse) {
        return (null != syncResponse.deviceLists)
                && (null != syncResponse.deviceLists.changed)
                && (syncResponse.deviceLists.changed.size() > 0);
    }


    /**
     * Use a broadcast receiver because the Timer delay might be inaccurate when the screen is turned off.
     * For example, request a 1 min delay and get a 6 mins one.
     */
    public static class SyncDelayReceiver extends BroadcastReceiver {
        public static final String EXTRA_INSTANCE_ID = "EXTRA_INSTANCE_ID";

        public void onReceive(Context context, Intent intent) {
            String instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID);

            if ((null != instanceId) && mSyncObjectByInstance.containsKey(instanceId)) {
                EventsThread eventsThread = mSyncObjectByInstance.get(instanceId);

                eventsThread.mPendingDelayedIntent = null;

                Log.d(LOG_TAG, "start a sync after " + eventsThread.mRequestDelayMs + " ms");

                synchronized (eventsThread.mSyncObject) {
                    eventsThread.mSyncObject.notify();
                }
            }
        }
    }

    private void resumeInitialSync() {
        Log.d(LOG_TAG, "Resuming initial sync from " + mCurrentToken);
        // dummy initial sync
        // to hide the splash screen
        SyncResponse dummySyncResponse = new SyncResponse();
        dummySyncResponse.nextBatch = mCurrentToken;
        mListener.onSyncResponse(dummySyncResponse, null, true);
    }

    private void executeInitialSync() {
        Log.d(LOG_TAG, "Requesting initial sync...");
        long initialSyncStartTime = System.currentTimeMillis();
        while (!isInitialSyncDone()) {
            final CountDownLatch latch = new CountDownLatch(1);
            mEventsRestClient.syncFromToken(null, 0, DEFAULT_CLIENT_TIMEOUT_MS, mIsOnline ? null : "offline", mFilterOrFilterId,
                    new SimpleApiCallback<SyncResponse>(mFailureCallback) {
                        @Override
                        public void onSuccess(SyncResponse syncResponse) {
                            Log.d(LOG_TAG, "Received initial sync response.");
                            mNextServerTimeoutms = hasDevicesChanged(syncResponse) ? 0 : mDefaultServerTimeoutms;
                            mListener.onSyncResponse(syncResponse, null, (0 == mNextServerTimeoutms));
                            mCurrentToken = syncResponse.nextBatch;
                            // unblock the events thread
                            latch.countDown();
                        }

                        private void sleepAndUnblock() {
                            Log.i(LOG_TAG, "Waiting a bit before retrying");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                public void run() {
                                    latch.countDown();
                                }
                            }, RETRY_WAIT_TIME_MS);
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            if (isInitialSyncDone()) {
                                // Ignore error
                                // FIXME I think this is the source of infinite initial sync if a network error occurs
                                // FIXME because latch is not counted down. TO BE TESTED
                                onSuccess(null);
                            } else {
                                Log.e(LOG_TAG, "Sync V2 onNetworkError " + e.getMessage(), e);
                                super.onNetworkError(e);
                                sleepAndUnblock();
                            }
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            super.onMatrixError(e);

                            if (MatrixError.isConfigurationErrorCode(e.errcode)) {
                                mListener.onConfigurationError(e.errcode);
                            } else {
                                mListener.onSyncError(e);
                                sleepAndUnblock();
                            }
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            super.onUnexpectedError(e);
                            Log.e(LOG_TAG, "Sync V2 onUnexpectedError " + e.getMessage(), e);
                            sleepAndUnblock();
                        }
                    });

            // block until the initial sync callback is invoked.
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Interrupted whilst performing initial sync.", e);
            } catch (Exception e) {
                // reported by GA
                // The thread might have been killed.
                Log.e(LOG_TAG, "latch.await() failed " + e.getMessage(), e);
            }
        }
        long initialSyncEndTime = System.currentTimeMillis();
        long initialSyncDuration = initialSyncEndTime - initialSyncStartTime;
        if (mMetricsListener != null) {
            mMetricsListener.onInitialSyncFinished(initialSyncDuration);
        }
    }


    /**
     * Start the events sync
     */
    @SuppressLint("NewApi")
    private void startSync() {
        int serverTimeout;
        mPaused = false;
        if (isInitialSyncDone()) {
            resumeInitialSync();
            serverTimeout = 0;
        } else {
            // Start with initial sync
            executeInitialSync();
            serverTimeout = mNextServerTimeoutms;
        }

        Log.d(LOG_TAG, "Starting event stream from token " + mCurrentToken);
        // sanity check
        if (null != mNetworkConnectivityReceiver) {
            mNetworkConnectivityReceiver.addEventListener(mNetworkListener);
            //
            mbIsConnected = mNetworkConnectivityReceiver.isConnected();
            mIsNetworkSuspended = !mbIsConnected;
        }

        // Then repeatedly long-poll for events

        while (!mKilling) {

            // test if a delay between two syncs
            if ((!mPaused && !mIsNetworkSuspended) && (0 != mRequestDelayMs)) {
                Log.d(LOG_TAG, "startSync : start a delay timer ");

                Intent intent = new Intent(mContext, SyncDelayReceiver.class);
                intent.putExtra(SyncDelayReceiver.EXTRA_INSTANCE_ID, toString());
                mPendingDelayedIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                long futureInMillis = SystemClock.elapsedRealtime() + mRequestDelayMs;

                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        && mPowerManager.isIgnoringBatteryOptimizations(mContext.getPackageName())) {
                    mAlarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, mPendingDelayedIntent);
                } else {
                    mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, mPendingDelayedIntent);
                }
            }

            if (mPaused || mIsNetworkSuspended || (null != mPendingDelayedIntent)) {
                if (null != mPendingDelayedIntent) {
                    Log.d(LOG_TAG, "Event stream is paused because there is a timer delay.");
                } else if (mIsNetworkSuspended) {
                    Log.d(LOG_TAG, "Event stream is paused because there is no available network.");
                } else {
                    Log.d(LOG_TAG, "Event stream is paused. Waiting.");
                }

                try {
                    Log.d(LOG_TAG, "startSync : wait ...");

                    synchronized (mSyncObject) {
                        mSyncObject.wait();
                    }

                    if (null != mPendingDelayedIntent) {
                        Log.d(LOG_TAG, "startSync : cancel mSyncDelayTimer");
                        mAlarmManager.cancel(mPendingDelayedIntent);
                        mPendingDelayedIntent.cancel();
                        mPendingDelayedIntent = null;
                    }

                    Log.d(LOG_TAG, "Event stream woken from pause.");

                    // perform a catchup asap
                    serverTimeout = 0;
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Unexpected interruption while paused: " + e.getMessage(), e);
                }
            }

            // the service could have been killed while being paused.
            if (!mKilling) {

                long incrementalSyncStartTime = System.currentTimeMillis();

                final CountDownLatch latch = new CountDownLatch(1);

                Log.d(LOG_TAG, "Get events from token " + mCurrentToken + " with filterOrFilterId " + mFilterOrFilterId);

                final int fServerTimeout = serverTimeout;
                mNextServerTimeoutms = mDefaultServerTimeoutms;

                mEventsRestClient.syncFromToken(mCurrentToken, serverTimeout, DEFAULT_CLIENT_TIMEOUT_MS, mIsOnline ? null : "offline", mFilterOrFilterId,
                        new SimpleApiCallback<SyncResponse>(mFailureCallback) {
                            @Override
                            public void onSuccess(SyncResponse syncResponse) {
                                if (!mKilling) {
                                    // poll /sync with timeout=0 until
                                    // we get no to_device messages back.
                                    if (0 == fServerTimeout) {
                                        if (hasDevicesChanged(syncResponse)) {
                                            if (mIsCatchingUp) {
                                                Log.d(LOG_TAG, "Some devices have changed but do not set mNextServerTimeoutms to 0 to avoid infinite loops");
                                            } else {
                                                Log.d(LOG_TAG, "mNextServerTimeoutms is set to 0 because of hasDevicesChanged "
                                                        + syncResponse.deviceLists.changed);
                                                mNextServerTimeoutms = 0;
                                            }
                                        }
                                    }

                                    // the catchup request is suspended when there is no need
                                    // to loop again
                                    if (mIsCatchingUp && (0 != mNextServerTimeoutms)) {
                                        // the catchup triggers sync requests until there are some useful events
                                        int eventCounts = 0;

                                        if (null != syncResponse.rooms) {
                                            RoomsSyncResponse roomsSyncResponse = syncResponse.rooms;

                                            if (null != roomsSyncResponse.join) {
                                                eventCounts += roomsSyncResponse.join.size();
                                            }

                                            if (null != roomsSyncResponse.invite) {
                                                eventCounts += roomsSyncResponse.invite.size();
                                            }
                                        }

                                        // stop any catch up
                                        mIsCatchingUp = false;
                                        mPaused = (0 == mRequestDelayMs);
                                        Log.d(LOG_TAG, "Got " + eventCounts + " useful events while catching up : mPaused is set to " + mPaused);
                                    }
                                    Log.d(LOG_TAG, "Got event response");
                                    mListener.onSyncResponse(syncResponse, mCurrentToken, (0 == mNextServerTimeoutms));
                                    mCurrentToken = syncResponse.nextBatch;
                                    Log.d(LOG_TAG, "mCurrentToken is now set to " + mCurrentToken);

                                }

                                // unblock the events thread
                                latch.countDown();
                            }

                            private void onError(String description) {
                                boolean isConnected;
                                Log.d(LOG_TAG, "Got an error while polling events " + description);

                                synchronized (mSyncObject) {
                                    isConnected = mbIsConnected;
                                }

                                // detected if the device is connected before trying again
                                if (isConnected) {
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        public void run() {
                                            latch.countDown();
                                        }
                                    }, RETRY_WAIT_TIME_MS);

                                } else {
                                    // no network -> wait that a network connection comes back.
                                    mIsNetworkSuspended = true;
                                    latch.countDown();
                                }
                            }

                            @Override
                            public void onNetworkError(Exception e) {
                                onError(e.getLocalizedMessage());
                            }

                            @Override
                            public void onMatrixError(MatrixError e) {
                                if (MatrixError.isConfigurationErrorCode(e.errcode)) {
                                    mListener.onConfigurationError(e.errcode);
                                } else {
                                    mListener.onSyncError(e);
                                    onError(e.getLocalizedMessage());
                                }
                            }

                            @Override
                            public void onUnexpectedError(Exception e) {
                                onError(e.getLocalizedMessage());
                            }
                        });

                // block until the sync callback is invoked.
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Interrupted whilst polling message", e);
                } catch (Exception e) {
                    // reported by GA
                    // The thread might have been killed.
                    Log.e(LOG_TAG, "latch.await() failed " + e.getMessage(), e);
                }
                long incrementalSyncEndTime = System.currentTimeMillis();
                long incrementalSyncDuration = incrementalSyncEndTime - incrementalSyncStartTime;
                if (mMetricsListener != null) {
                    mMetricsListener.onIncrementalSyncFinished(incrementalSyncDuration);
                }
            }
            serverTimeout = mNextServerTimeoutms;
        }

        if (null != mNetworkConnectivityReceiver) {
            mNetworkConnectivityReceiver.removeEventListener(mNetworkListener);
        }
        Log.d(LOG_TAG, "Event stream terminating.");
    }

    /**
     * Ask if the initial sync is done. It means we have a sync token
     *
     * @return
     */
    private boolean isInitialSyncDone() {
        return mCurrentToken != null;
    }
}
