/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.features.workers.signout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.matrix.android.api.session.Session

class SignOutViewModel : ViewModel() { // TODO, KeysBackupStateManager.KeysBackupStateListener {
    // Keys exported manually
    var keysExportedToFile = MutableLiveData<Boolean>()

    // var keysBackupState = MutableLiveData<KeysBackupStateManager.KeysBackupState>()

    private var mxSession: Session? = null

    fun init(session: Session) {
        if (mxSession == null) {
            mxSession = session

            // TODO
            //mxSession?.crypto
            //        ?.keysBackup
            //        ?.addListener(this)
        }

        //keysBackupState.value = mxSession?.crypto
        //        ?.keysBackup
        //        ?.state
    }

//    /**
//     * Safe way to get the current KeysBackup version
//     */
//    fun getCurrentBackupVersion(): String {
//        return mxSession
//                ?.crypto
//                ?.keysBackup
//                ?.currentBackupVersion
//                ?: ""
//    }
//
//    /**
//     * Safe way to get the number of keys to backup
//     */
//    fun getNumberOfKeysToBackup(): Int {
//        return mxSession
//                ?.crypto
//                ?.cryptoStore
//                ?.inboundGroupSessionsCount(false)
//                ?: 0
//    }
//
//    /**
//     * Safe way to tell if there are more keys on the server
//     */
//    fun canRestoreKeys(): Boolean {
//        return mxSession
//                ?.crypto
//                ?.keysBackup
//                ?.canRestoreKeys() == true
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//
//        mxSession?.crypto
//                ?.keysBackup
//                ?.removeListener(this)
//    }
//
//    override fun onStateChange(newState: KeysBackupStateManager.KeysBackupState) {
//        keysBackupState.value = newState
//    }

    companion object {
        /**
         * The backup check on logout flow has to be displayed if there are keys in the store, and the keys backup state is not Ready
         */
        fun doYouNeedToBeDisplayed(session: Session?): Boolean {
            return false

            /* TODO
            return session
                    ?.crypto
                    ?.cryptoStore
                    ?.inboundGroupSessionsCount(false)
                    ?: 0 > 0
                    && session
                    ?.crypto
                    ?.keysBackup
                    ?.state != KeysBackupStateManager.KeysBackupState.ReadyToBackUp
                    */
        }
    }
}