/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.settings.data

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.bumptech.glide.load.engine.cache.DiskCache
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.rageshake.VectorFileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

data class StorageUsageViewState(
        val sizeOfLogs: Async<Long> = Uninitialized,
        val sizeOfMediaAndFiles: Async<Long> = Uninitialized,
        val sizeOfCache: Async<Long> = Uninitialized,
        val sizeOfSessionDatabase: Async<Long> = Uninitialized,
        val sizeOfCryptoDatabase: Async<Long> = Uninitialized
) : MvRxState

sealed class StorageUsageViewEvents : VectorViewEvents

sealed class StorageUsageViewModelAction : VectorViewModelAction {
    object ClearMediaCache : StorageUsageViewModelAction()
}

class StorageUsageViewModel @AssistedInject constructor(
        @Assisted initialState: StorageUsageViewState,
        private val session: Session,
        private val vectorFileLogger: VectorFileLogger
) : VectorViewModel<StorageUsageViewState, StorageUsageViewModelAction, StorageUsageViewEvents>(initialState) {

    init {

        viewModelScope.launch(Dispatchers.IO) {
            val dlSize = session.fileService().getCacheSize().toLong()

            setState {
                copy(
                        sizeOfMediaAndFiles = Success(dlSize)
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val logSize = vectorFileLogger.getLogSize().toLong()
            setState {
                copy(
                        sizeOfLogs = Success(logSize)
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val glideCacheSize = session.storageUsageService().cacheDirectorySize(DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            setState {
                copy(
                        sizeOfCache = Success(glideCacheSize)
                )
            }

            val cyptoSize = session.storageUsageService().cryptoDataBaseSize()
            val sessionSize = session.storageUsageService().sessionDataBaseSize()

            setState {
                copy(
                        sizeOfCryptoDatabase = Success(cyptoSize),
                        sizeOfSessionDatabase = Success(sessionSize)
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: StorageUsageViewState): StorageUsageViewModel
    }

    companion object : MvRxViewModelFactory<StorageUsageViewModel, StorageUsageViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: StorageUsageViewState): StorageUsageViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: StorageUsageViewModelAction) {
        when (action) {
            StorageUsageViewModelAction.ClearMediaCache -> {
            }
        }.exhaustive
    }
}

class DataAndStorageFragment @Inject constructor(
        val viewModelFactory: StorageUsageViewModel.Factory,
        private val epoxyController: StorageUsageController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(), StorageUsageViewModel.Factory {

    private val viewModel: StorageUsageViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    override fun create(initialState: StorageUsageViewState) = viewModelFactory.create(initialState)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.genericRecyclerView.configureWith(epoxyController, showDivider = true)
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }
}
