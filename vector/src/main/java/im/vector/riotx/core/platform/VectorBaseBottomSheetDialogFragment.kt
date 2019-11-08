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
package im.vector.riotx.core.platform

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.MvRxView
import com.airbnb.mvrx.MvRxViewModelStore
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import im.vector.riotx.core.di.DaggerScreenComponent
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.utils.DimensionConverter
import java.util.*

/**
 * Add MvRx capabilities to bottomsheetdialog (like BaseMvRxFragment)
 */
abstract class VectorBaseBottomSheetDialogFragment : BottomSheetDialogFragment(), MvRxView {

    override val mvrxViewModelStore by lazy { MvRxViewModelStore(viewModelStore) }
    private lateinit var mvrxPersistedViewId: String
    private lateinit var screenComponent: ScreenComponent
    final override val mvrxViewId: String by lazy { mvrxPersistedViewId }

    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val activityViewModelProvider
        get() = ViewModelProviders.of(requireActivity(), viewModelFactory)

    protected val fragmentViewModelProvider
        get() = ViewModelProviders.of(this, viewModelFactory)

    /* ==========================================================================================
     * BottomSheetBehavior
     * ========================================================================================== */

    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    val vectorBaseActivity: VectorBaseActivity by lazy {
        activity as VectorBaseActivity
    }

    open val showExpanded = false

    override fun onAttach(context: Context) {
        screenComponent = DaggerScreenComponent.factory().create(vectorBaseActivity.getVectorComponent(), vectorBaseActivity)
        viewModelFactory = screenComponent.viewModelFactory()
        super.onAttach(context)
        injectWith(screenComponent)
    }

    protected open fun injectWith(screenComponent: ScreenComponent) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        mvrxViewModelStore.restoreViewModels(this, savedInstanceState)
        mvrxPersistedViewId = savedInstanceState?.getString(PERSISTED_VIEW_ID_KEY)
                ?: this::class.java.simpleName + "_" + UUID.randomUUID().toString()

        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            val dialog = this as? BottomSheetDialog
            bottomSheetBehavior = dialog?.behavior
            bottomSheetBehavior?.setPeekHeight(DimensionConverter(resources).dpToPx(400), false)
            if (showExpanded) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mvrxViewModelStore.saveViewModels(outState)
        outState.putString(PERSISTED_VIEW_ID_KEY, mvrxViewId)
    }

    override fun onStart() {
        super.onStart()
        // This ensures that invalidate() is called for static screens that don't
        // subscribe to a ViewModel.
        postInvalidate()
    }

    @CallSuper
    override fun invalidate() {
        if (showExpanded) {
            // Force the bottom sheet to be expanded
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    protected fun setArguments(args: Parcelable? = null) {
        arguments = args?.let { Bundle().apply { putParcelable(MvRx.KEY_ARG, it) } }
    }
}

private const val PERSISTED_VIEW_ID_KEY = "mvrx:bottomsheet_persisted_view_id"
