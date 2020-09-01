/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class ActiveCallHandler @Inject constructor() {

    private val activeCallListLiveData: MutableLiveData<MutableList<MxCall>> by lazy {
        MutableLiveData<MutableList<MxCall>>(mutableListOf())
    }

    fun addCall(call: MxCall) {
        activeCallListLiveData.postValue(activeCallListLiveData.value?.apply { add(call) })
    }

    fun removeCall(callId: String) {
        activeCallListLiveData.postValue(activeCallListLiveData.value?.apply { removeAll { it.callId == callId } })
    }

    fun getCallWithId(callId: String): MxCall? {
        return activeCallListLiveData.value?.find { it.callId == callId }
    }

    fun getActiveCallsLiveData(): LiveData<MutableList<MxCall>> = activeCallListLiveData
}
