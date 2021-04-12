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

package im.vector.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toModel
import javax.inject.Inject
import javax.inject.Singleton
import org.matrix.android.sdk.rx.rx

/**
 * This class handles the global app state.
 * It requires to be added to ProcessLifecycleOwner.get().lifecycle
 */
// TODO Keep this class for now, will maybe be used fro Space
@Singleton
class AppStateHandler @Inject constructor(
        private val sessionDataSource: ActiveSessionDataSource,
        private val matrixItemColorProvider: MatrixItemColorProvider) : LifecycleObserver {

    private val compositeDisposable = CompositeDisposable()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun entersForeground() {
        observeUserAccountData()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun entersBackground() {
        compositeDisposable.clear()
    }

    private fun observeUserAccountData() {
        sessionDataSource.observe()
                .observeOn(AndroidSchedulers.mainThread())
                .switchMap {
                    it.orNull()?.rx()?.liveAccountData(setOf(UserAccountDataTypes.TYPE_OVERRIDE_COLORS))
                            ?: Observable.just(emptyList())
                }
                .distinctUntilChanged()
                .subscribe {
                    val overrideColorSpecs = it?.firstOrNull()?.content?.toModel<Map<String, String>>()
                    matrixItemColorProvider.setOverrideColors(overrideColorSpecs)
                }
                .addTo(compositeDisposable)
    }
}
