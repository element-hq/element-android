/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.di

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel

/**
 * This factory allows Mavericks to supply the initial or restored [MavericksState] to Hilt.
 *
 * Add this interface inside of your [MavericksViewModel] class then create the following Hilt module:
 *
 * @Module
 * @InstallIn(MavericksViewModelComponent::class)
 * interface ViewModelsModule {
 *   @Binds
 *   @IntoMap
 *   @ViewModelKey(MyViewModel::class)
 *   fun myViewModelFactory(factory: MyViewModel.Factory): AssistedViewModelFactory<*, *>
 * }
 *
 * If you already have a ViewModelsModule then all you have to do is add the multibinding entry for your new [MavericksViewModel].
 */
interface MavericksAssistedViewModelFactory<VM : MavericksViewModel<S>, S : MavericksState> {
    fun create(initialState: S): VM
}
