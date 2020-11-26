/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.ui.bottomsheet

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import javax.inject.Inject

/**
 * Generic Bottom sheet with actions
 */
abstract class BottomSheetGeneric<STATE : BottomSheetGenericState, ACTION : BottomSheetGenericAction> :
        VectorBaseBottomSheetDialogFragment(),
        BottomSheetGenericController.Listener<ACTION> {

    @Inject lateinit var sharedViewPool: RecyclerView.RecycledViewPool

    @BindView(R.id.bottomSheetRecyclerView)
    lateinit var recyclerView: RecyclerView

    final override val showExpanded = true

    final override fun getLayoutResId() = R.layout.bottom_sheet_generic_list

    abstract fun getController(): BottomSheetGenericController<STATE, ACTION>

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.configureWith(getController(), viewPool = sharedViewPool, hasFixedSize = false, disableItemAnimation = true)
        getController().listener = this
    }

    @CallSuper
    override fun onDestroyView() {
        recyclerView.cleanup()
        getController().listener = null
        super.onDestroyView()
    }
}
