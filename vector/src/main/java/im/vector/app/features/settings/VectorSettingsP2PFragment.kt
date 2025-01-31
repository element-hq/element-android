/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.preference.VectorEditTextPreference
import im.vector.app.core.preference.VectorSwitchPreference
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsP2PFragment :
        VectorSettingsBaseFragment() {

    @Inject lateinit var vectorPreferences: VectorPreferences

    override var titleRes = R.string.settings_p2p_title
    override val preferenceXmlRes = R.xml.vector_settings_p2p

    private val mMulticastPeersEnabled by lazy {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_P2P_ENABLE_MULTICAST)!!
    }
    private val mBluetoothPeersEnabled by lazy {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_P2P_ENABLE_BLUETOOTH)!!
    }
    private val mStaticPeerEnabled by lazy {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_P2P_ENABLE_STATIC)!!
    }
    private val mStaticPeerURI by lazy {
        findPreference<VectorEditTextPreference>(VectorPreferences.SETTINGS_P2P_STATIC_URI)!!
    }
    private val mRelayingEnabled by lazy {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_P2P_ENABLE_RELAYING)!!
    }
    private val mSelfRelayURI by lazy {
        findPreference<VectorEditTextPreference>(VectorPreferences.SETTINGS_P2P_SELF_RELAY_URI)!!
    }
    private val mBLECodedPhy by lazy {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_P2P_BLE_CODED_PHY)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMulticastPeersEnabled.isChecked = vectorPreferences.p2pEnableMulticast()
        mBluetoothPeersEnabled.isChecked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vectorPreferences.p2pEnableBluetooth()
        mStaticPeerEnabled.isChecked = vectorPreferences.p2pEnableStatic()
        mStaticPeerURI.summary = vectorPreferences.p2pStaticURI().ifEmpty { "No static peer is configured" }
        mRelayingEnabled.isChecked = vectorPreferences.p2pRelayingEnabled()
        mSelfRelayURI.summary = vectorPreferences.p2pSelfRelayURI().ifEmpty { "No relay server is configured" }
        mBLECodedPhy.isChecked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vectorPreferences.p2pBLECodedPhy()

        mBluetoothPeersEnabled.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            mBLECodedPhy.isEnabled = bluetoothManager?.adapter?.isLeCodedPhySupported ?: false
        } else {
            mBLECodedPhy.isEnabled = false
        }
    }

    override fun bindPref() {
        mStaticPeerURI.let {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                mStaticPeerURI.summary = newValue.toString().ifEmpty { "No static peer is configured" }
                true
            }
        }

        mSelfRelayURI.let {
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                mSelfRelayURI.summary = newValue.toString().ifEmpty { "No relay server is configured" }
                true
            }
        }
    }
}
