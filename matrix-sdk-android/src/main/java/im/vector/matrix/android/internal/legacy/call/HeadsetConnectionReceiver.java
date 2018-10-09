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

package im.vector.matrix.android.internal.legacy.call;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.HashSet;
import java.util.Set;

// this class detect if the headset is plugged / unplugged
public class HeadsetConnectionReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = HeadsetConnectionReceiver.class.getSimpleName();

    private static Boolean mIsHeadsetPlugged = null;

    private static HeadsetConnectionReceiver mSharedInstance = null;

    /**
     * Track the headset update.
     */
    public interface OnHeadsetStatusUpdateListener {
        /**
         * A wire headset has been plugged / unplugged.
         *
         * @param isPlugged true if the headset is now plugged.
         */
        void onWiredHeadsetUpdate(boolean isPlugged);

        /**
         * A bluetooth headset is connected.
         *
         * @param isConnected true if the bluetooth headset is connected.
         */
        void onBluetoothHeadsetUpdate(boolean isConnected);

    }

    // listeners
    private final Set<OnHeadsetStatusUpdateListener> mListeners = new HashSet<>();

    public HeadsetConnectionReceiver() {
    }

    /**
     * @param context the application context
     * @return the shared instance
     */
    public static HeadsetConnectionReceiver getSharedInstance(Context context) {
        if (null == mSharedInstance) {
            mSharedInstance = new HeadsetConnectionReceiver();
            context.registerReceiver(mSharedInstance, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            context.registerReceiver(mSharedInstance, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
            context.registerReceiver(mSharedInstance, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            context.registerReceiver(mSharedInstance, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
            context.registerReceiver(mSharedInstance, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        }

        return mSharedInstance;
    }

    /**
     * Add a listener.
     *
     * @param listener the listener to add.
     */
    public void addListener(OnHeadsetStatusUpdateListener listener) {
        synchronized (LOG_TAG) {
            mListeners.add(listener);
        }
    }

    /**
     * Remove a listener.
     *
     * @param listener the listener to remove.
     */
    public void removeListener(OnHeadsetStatusUpdateListener listener) {
        synchronized (LOG_TAG) {
            mListeners.remove(listener);
        }
    }

    /**
     * Dispatch onBluetoothHeadsetUpdate to the listeners.
     *
     * @param isConnected true if a bluetooth headset is connected.
     */
    private void onBluetoothHeadsetUpdate(boolean isConnected) {
        synchronized (LOG_TAG) {
            for (OnHeadsetStatusUpdateListener listener : mListeners) {
                try {
                    listener.onBluetoothHeadsetUpdate(isConnected);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## onBluetoothHeadsetUpdate()) failed " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Dispatch onWireHeadsetUpdate to the listeners.
     *
     * @param isPlugged true if the wire headset is plugged.
     */
    private void onWiredHeadsetUpdate(boolean isPlugged) {
        synchronized (LOG_TAG) {
            for (OnHeadsetStatusUpdateListener listener : mListeners) {
                try {
                    listener.onWiredHeadsetUpdate(isPlugged);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## onWiredHeadsetUpdate()) failed " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void onReceive(final Context aContext, final Intent aIntent) {
        Log.d(LOG_TAG, "## onReceive() : " + aIntent.getExtras());
        String action = aIntent.getAction();

        if (TextUtils.equals(action, Intent.ACTION_HEADSET_PLUG)
                || TextUtils.equals(action, BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                || TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)
                || TextUtils.equals(action, BluetoothDevice.ACTION_ACL_CONNECTED)
                || TextUtils.equals(action, BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

            Boolean newState = null;
            final boolean isBTHeadsetUpdate;

            if (TextUtils.equals(action, Intent.ACTION_HEADSET_PLUG)) {
                int state = aIntent.getIntExtra("state", -1);

                switch (state) {
                    case 0:
                        Log.d(LOG_TAG, "Headset is unplugged");
                        newState = false;
                        break;
                    case 1:
                        Log.d(LOG_TAG, "Headset is plugged");
                        newState = true;
                        break;
                    default:
                        Log.d(LOG_TAG, "undefined state");
                }
                isBTHeadsetUpdate = false;
            } else {
                int state = BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(BluetoothProfile.HEADSET);

                Log.d(LOG_TAG, "bluetooth headset state " + state);
                newState = (BluetoothAdapter.STATE_CONNECTED == state);
                isBTHeadsetUpdate = mIsHeadsetPlugged != newState;
            }

            if (newState != mIsHeadsetPlugged) {
                mIsHeadsetPlugged = newState;

                // wait a little else route to BT headset does not work.
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isBTHeadsetUpdate) {
                            onBluetoothHeadsetUpdate(mIsHeadsetPlugged);
                        } else {
                            onWiredHeadsetUpdate(mIsHeadsetPlugged);
                        }
                    }
                }, 1000);
            }
        }
    }

    private static AudioManager mAudioManager = null;

    /**
     * @return the audio manager
     */
    private static AudioManager getAudioManager(Context context) {
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        return mAudioManager;
    }


    /**
     * @param context the context
     * @return true if the headset is plugged
     */
    @SuppressLint("Deprecation")
    public static boolean isHeadsetPlugged(Context context) {
        if (null == mIsHeadsetPlugged) {
            AudioManager audioManager = getAudioManager(context);
            mIsHeadsetPlugged = isBTHeadsetPlugged() || audioManager.isWiredHeadsetOn();
        }

        return mIsHeadsetPlugged;
    }

    /**
     * @return true if bluetooth headset is plugged
     */
    public static boolean isBTHeadsetPlugged() {
        return (BluetoothAdapter.STATE_CONNECTED == BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(BluetoothProfile.HEADSET));
    }
}
