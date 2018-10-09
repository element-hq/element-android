/*
 * Copyright 2015 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.network;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import im.vector.matrix.android.internal.legacy.util.Log;

import im.vector.matrix.android.internal.legacy.listeners.IMXNetworkEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class NetworkConnectivityReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = NetworkConnectivityReceiver.class.getSimpleName();

    // any network state listener
    private final List<IMXNetworkEventListener> mNetworkEventListeners = new ArrayList<>();

    // the one call listeners are listeners which are expected to be called ONCE
    // the device is connected to a data network
    private final List<IMXNetworkEventListener> mOnNetworkConnectedEventListeners = new ArrayList<>();

    private boolean mIsConnected = false;
    private boolean mIsUseWifiConnection = false;
    private int mNetworkSubType = TelephonyManager.NETWORK_TYPE_UNKNOWN;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        NetworkInfo networkInfo = null;

        if (null != intent) {

            Log.d(LOG_TAG, "## onReceive() : action " + intent.getAction());

            Bundle extras = intent.getExtras();

            if (null != extras) {
                Set<String> keys = extras.keySet();

                for (String key : keys) {
                    Log.d(LOG_TAG, "## onReceive() : " + key + " -> " + extras.get(key));
                }

                if (extras.containsKey("networkInfo")) {
                    Object networkInfoAsVoid = extras.get("networkInfo");

                    if (networkInfoAsVoid instanceof NetworkInfo) {
                        networkInfo = (NetworkInfo) networkInfoAsVoid;
                    }
                }
            }
        } else {
            Log.d(LOG_TAG, "## onReceive()");
        }

        checkNetworkConnection(context, networkInfo);
    }

    /**
     * Check if there is a connection update.
     *
     * @param context the context
     */
    public void checkNetworkConnection(Context context) {
        checkNetworkConnection(context, null);
    }

    /**
     * Check if there is a connection update.
     *
     * @param context the context
     */
    private void checkNetworkConnection(Context context, NetworkInfo aNetworkInfo) {
        synchronized (LOG_TAG) {
            try {
                NetworkInfo networkInfo = aNetworkInfo;

                // https://issuetracker.google.com/issues/37137911
                // it seems that getActiveNetworkInfo does not provide the true active network connection
                if (null == networkInfo) {
                    ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    networkInfo = connMgr.getActiveNetworkInfo();
                }
                boolean isConnected = (networkInfo != null) && networkInfo.isConnectedOrConnecting();

                if (isConnected) {
                    Log.d(LOG_TAG, "## checkNetworkConnection() : Connected to " + networkInfo);
                } else if (null != networkInfo) {
                    Log.d(LOG_TAG, "## checkNetworkConnection() : there is a default connection but it is not connected " + networkInfo);
                    listNetworkConnections(context);
                } else {
                    Log.d(LOG_TAG, "## checkNetworkConnection() : there is no connection");
                    listNetworkConnections(context);
                }

                mIsUseWifiConnection = (null != networkInfo) && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
                mNetworkSubType = (null != networkInfo) ? networkInfo.getSubtype() : TelephonyManager.NETWORK_TYPE_UNKNOWN;

                // avoid triggering useless info
                if (mIsConnected != isConnected) {
                    Log.d(LOG_TAG, "## checkNetworkConnection() : Warn there is a connection update");
                    mIsConnected = isConnected;
                    onNetworkUpdate();
                } else {
                    Log.d(LOG_TAG, "## checkNetworkConnection() : No network update");
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to report :" + e.getMessage(), e);
            }
        }
    }

    /**
     * List the available network connections
     *
     * @param context the context
     */
    @SuppressLint("deprecation")
    private static void listNetworkConnections(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        List<NetworkInfo> networkInfos = new ArrayList<>();

        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] activeNetworks = cm.getAllNetworks();
            if (null != activeNetworks) {
                for (Network network : activeNetworks) {
                    NetworkInfo networkInfo = cm.getNetworkInfo(network);
                    if (null != networkInfo) {
                        networkInfos.add(networkInfo);
                    }
                }
            }
        } else {
            NetworkInfo[] info = cm.getAllNetworkInfo();

            if (info != null) {
                networkInfos.addAll(Arrays.asList(info));
            }
        }

        Log.d(LOG_TAG, "## listNetworkConnections() : " + networkInfos.size() + " connections");

        for (NetworkInfo networkInfo : networkInfos) {
            Log.d(LOG_TAG, "-> " + networkInfo);
        }
    }

    /**
     * Add a network event listener.
     *
     * @param networkEventListener the event listener to add
     */
    public void addEventListener(final IMXNetworkEventListener networkEventListener) {
        if (null != networkEventListener) {
            mNetworkEventListeners.add(networkEventListener);
        }
    }

    /**
     * Add a ONE CALL network event listener.
     * The listener is called when a data connection is established.
     * The listener is removed from the listeners list once its callback is called.
     *
     * @param networkEventListener the event listener to add
     */
    public void addOnConnectedEventListener(final IMXNetworkEventListener networkEventListener) {
        if (null != networkEventListener) {
            synchronized (LOG_TAG) {
                mOnNetworkConnectedEventListeners.add(networkEventListener);
            }
        }
    }

    /**
     * Remove a network event listener.
     *
     * @param networkEventListener the event listener to remove
     */
    public void removeEventListener(final IMXNetworkEventListener networkEventListener) {
        synchronized (LOG_TAG) {
            mNetworkEventListeners.remove(networkEventListener);
            mOnNetworkConnectedEventListeners.remove(networkEventListener);
        }
    }

    /**
     * Remove all registered listeners
     */
    public void removeListeners() {
        synchronized (LOG_TAG) {
            mNetworkEventListeners.clear();
            mOnNetworkConnectedEventListeners.clear();
        }
    }

    /**
     * Warn the listener that a network updated has been triggered
     */
    private synchronized void onNetworkUpdate() {
        for (IMXNetworkEventListener listener : mNetworkEventListeners) {
            try {
                listener.onNetworkConnectionUpdate(mIsConnected);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onNetworkUpdate() : onNetworkConnectionUpdate failed " + e.getMessage(), e);
            }
        }

        // onConnected listeners are called once
        // and only when there is an available network connection
        if (mIsConnected) {
            for (IMXNetworkEventListener listener : mOnNetworkConnectedEventListeners) {
                try {
                    listener.onNetworkConnectionUpdate(true);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## onNetworkUpdate() : onNetworkConnectionUpdate failed " + e.getMessage(), e);
                }
            }

            mOnNetworkConnectedEventListeners.clear();
        }
    }

    /**
     * @return true if the application is connected to a data network
     */
    public boolean isConnected() {
        boolean res;

        synchronized (LOG_TAG) {
            res = mIsConnected;
        }

        Log.d(LOG_TAG, "## isConnected() : " + res);

        return res;
    }


    /**
     * Tells if the connection is a wifi one
     *
     * @return true if a wifi connection is used
     */
    public boolean useWifiConnection() {
        boolean res;

        synchronized (LOG_TAG) {
            res = mIsUseWifiConnection;
        }

        Log.d(LOG_TAG, "## useWifiConnection() : " + res);

        return res;
    }

    /**
     * Provides a scale factor to apply to the request timeouts.
     *
     * @return the scale factor
     */
    public float getTimeoutScale() {
        float scale;

        synchronized (LOG_TAG) {
            switch (mNetworkSubType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    scale = 3.0f;
                    break;
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    scale = 2.5f;
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    scale = 2.0f;
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    scale = 1.5f;
                    break;
                default:
                    scale = 1.0f;
                    break;
            }
        }
        return scale;
    }
}
