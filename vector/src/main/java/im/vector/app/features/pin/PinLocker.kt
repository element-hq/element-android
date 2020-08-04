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

package im.vector.app.features.pin

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val PERIOD_OF_GRACE_IN_MS = 2 * 60 * 1000L

/**
 * This class is responsible for keeping the status of locking
 * It automatically locks when entering background/foreground with a grace period.
 * You can force to unlock with unlock method, use it whenever the pin code has been validated.
 */

@Singleton
class PinLocker @Inject constructor(private val pinCodeStore: PinCodeStore) : LifecycleObserver {

    enum class State {
        // App is locked, can be unlock
        LOCKED,

        // App is blocked and can't be unlocked as long as the app is in foreground
        BLOCKED,

        // is unlocked, the app can be used
        UNLOCKED
    }

    private val liveState = MutableLiveData<State>()

    private var isBlocked = false
    private var shouldBeLocked = true
    private var entersBackgroundTs = 0L

    fun getLiveState(): LiveData<State> {
        return liveState
    }

    private fun computeState() {
        GlobalScope.launch {
            val state = if (isBlocked) {
                State.BLOCKED
            } else if (shouldBeLocked && pinCodeStore.hasEncodedPin()) {
                State.LOCKED
            } else {
                State.UNLOCKED
            }
            if (liveState.value != state) {
                liveState.postValue(state)
            }
        }
    }

    fun unlock() {
        Timber.v("Unlock app")
        shouldBeLocked = false
        computeState()
    }

    fun block() {
        Timber.v("Block app")
        isBlocked = true
        computeState()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
        val timeElapsedSinceBackground = SystemClock.elapsedRealtime() - entersBackgroundTs
        shouldBeLocked = shouldBeLocked || timeElapsedSinceBackground >= PERIOD_OF_GRACE_IN_MS
        Timber.v("App enters foreground after $timeElapsedSinceBackground ms spent in background")
        computeState()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        isBlocked = false
        entersBackgroundTs = SystemClock.elapsedRealtime()
    }
}
