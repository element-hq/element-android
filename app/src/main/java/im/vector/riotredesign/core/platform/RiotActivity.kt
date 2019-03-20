/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.core.platform

import android.os.Bundle
import androidx.annotation.MainThread
import com.airbnb.mvrx.BaseMvRxActivity
import com.bumptech.glide.util.Util
import im.vector.riotredesign.BuildConfig
import im.vector.riotredesign.receivers.DebugReceiver
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class RiotActivity : BaseMvRxActivity() {

    // For debug only
    private var debugReceiver: DebugReceiver? = null

    private val uiDisposables = CompositeDisposable()
    private val restorables = ArrayList<Restorable>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        restorables.forEach { it.onSaveInstanceState(outState) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        restorables.forEach { it.onRestoreInstanceState(savedInstanceState) }
        super.onRestoreInstanceState(savedInstanceState)
    }

    @MainThread
    protected fun <T : Restorable> T.register(): T {
        Util.assertMainThread()
        restorables.add(this)
        return this
    }

    protected fun Disposable.disposeOnDestroy(): Disposable {
        uiDisposables.add(this)
        return this
    }

    override fun onResume() {
        super.onResume()

        DebugReceiver
                .getIntentFilter(this)
                .takeIf { BuildConfig.DEBUG }
                ?.let {
                    debugReceiver = DebugReceiver()
                    registerReceiver(debugReceiver, it)
                }
    }

    override fun onPause() {
        super.onPause()

        debugReceiver?.let {
            unregisterReceiver(debugReceiver)
            debugReceiver = null
        }
    }

}