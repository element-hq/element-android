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

package im.vector.riotx.features.settings.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.riotx.core.extensions.postLiveEvent
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction
import im.vector.riotx.core.utils.LiveEvent
import timber.log.Timber

data class DevicesViewState(
        val myDeviceId: String = "",
        val devices: Async<List<DeviceInfo>> = Uninitialized,
        val request: Async<Unit> = Uninitialized
) : MvRxState

sealed class DevicesAction : VectorViewModelAction {
    object Retry : DevicesAction()
    data class Delete(val deviceInfo: DeviceInfo) : DevicesAction()
    data class Password(val password: String) : DevicesAction()
    data class Rename(val deviceInfo: DeviceInfo, val newName: String) : DevicesAction()
}

class DevicesViewModel @AssistedInject constructor(@Assisted initialState: DevicesViewState,
                                                   private val session: Session)
    : VectorViewModel<DevicesViewState, DevicesAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MvRxViewModelFactory<DevicesViewModel, DevicesViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DevicesViewState): DevicesViewModel? {
            val fragment: VectorSettingsDevicesFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.devicesViewModelFactory.create(state)
        }
    }

    // temp storage when we ask for the user password
    private var _currentDeviceId: String? = null
    private var _currentSession: String? = null

    private val _requestPasswordLiveData = MutableLiveData<LiveEvent<Unit>>()
    val requestPasswordLiveData: LiveData<LiveEvent<Unit>>
        get() = _requestPasswordLiveData

    init {
        refreshDevicesList()
    }

    /**
     * Force the refresh of the devices list.
     * The devices list is the list of the devices where the user as logged in.
     * It can be any mobile device, as any browser.
     */
    private fun refreshDevicesList() {
        if (session.isCryptoEnabled() && !session.sessionParams.credentials.deviceId.isNullOrEmpty()) {
            setState {
                copy(
                        devices = Loading()
                )
            }

            session.getDevicesList(object : MatrixCallback<DevicesListResponse> {
                override fun onSuccess(data: DevicesListResponse) {
                    setState {
                        copy(
                                myDeviceId = session.sessionParams.credentials.deviceId ?: "",
                                devices = Success(data.devices.orEmpty())
                        )
                    }
                }

                override fun onFailure(failure: Throwable) {
                    setState {
                        copy(
                                devices = Fail(failure)
                        )
                    }
                }
            })
        } else {
            // Should not happen
        }
    }

    override fun handle(action: DevicesAction) {
        return when (action) {
            is DevicesAction.Retry    -> refreshDevicesList()
            is DevicesAction.Delete   -> handleDelete(action)
            is DevicesAction.Password -> handlePassword(action)
            is DevicesAction.Rename   -> handleRename(action)
        }
    }

    private fun handleRename(action: DevicesAction.Rename) {
        session.setDeviceName(action.deviceInfo.deviceId!!, action.newName, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                setState {
                    copy(
                            request = Success(data)
                    )
                }
                // force settings update
                refreshDevicesList()
            }

            override fun onFailure(failure: Throwable) {
                setState {
                    copy(
                            request = Fail(failure)
                    )
                }

                _requestErrorLiveData.postLiveEvent(failure)
            }
        })
    }

    /**
     * Try to delete a device.
     */
    private fun handleDelete(action: DevicesAction.Delete) {
        val deviceId = action.deviceInfo.deviceId
        if (deviceId == null) {
            Timber.e("## handleDelete(): sanity check failure")
            return
        }

        setState {
            copy(
                    request = Loading()
            )
        }

        session.deleteDevice(deviceId, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                var isPasswordRequestFound = false

                if (failure is Failure.RegistrationFlowError) {
                    // We only support LoginFlowTypes.PASSWORD
                    // Check if we can provide the user password
                    failure.registrationFlowResponse.flows?.forEach { interactiveAuthenticationFlow ->
                        isPasswordRequestFound = isPasswordRequestFound || interactiveAuthenticationFlow.stages?.any { it == LoginFlowTypes.PASSWORD } == true
                    }

                    if (isPasswordRequestFound) {
                        _currentDeviceId = deviceId
                        _currentSession = failure.registrationFlowResponse.session

                        setState {
                            copy(
                                    request = Success(Unit)
                            )
                        }

                        _requestPasswordLiveData.postLiveEvent(Unit)
                    }
                }

                if (!isPasswordRequestFound) {
                    // LoginFlowTypes.PASSWORD not supported, and this is the only one RiotX supports so far...
                    setState {
                        copy(
                                request = Fail(failure)
                        )
                    }

                    _requestErrorLiveData.postLiveEvent(failure)
                }
            }

            override fun onSuccess(data: Unit) {
                setState {
                    copy(
                            request = Success(data)
                    )
                }
                // force settings update
                refreshDevicesList()
            }
        })
    }

    private fun handlePassword(action: DevicesAction.Password) {
        val currentDeviceId = _currentDeviceId
        if (currentDeviceId.isNullOrBlank()) {
            // Abort
            return
        }

        setState {
            copy(
                    request = Loading()
            )
        }

        session.deleteDeviceWithUserPassword(currentDeviceId, _currentSession, action.password, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _currentDeviceId = null
                _currentSession = null

                setState {
                    copy(
                            request = Success(data)
                    )
                }
                // force settings update
                refreshDevicesList()
            }

            override fun onFailure(failure: Throwable) {
                _currentDeviceId = null
                _currentSession = null

                // Password is maybe not good
                setState {
                    copy(
                            request = Fail(failure)
                    )
                }

                _requestErrorLiveData.postLiveEvent(failure)
            }
        })
    }
}
