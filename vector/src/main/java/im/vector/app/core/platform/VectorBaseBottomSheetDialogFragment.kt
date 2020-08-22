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
package im.vector.app.core.platform

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.lifecycle.ViewModelProvider
import butterknife.ButterKnife
import butterknife.Unbinder
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.MvRxView
import com.airbnb.mvrx.MvRxViewId
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jakewharton.rxbinding3.view.clicks
import im.vector.app.core.di.DaggerScreenComponent
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.utils.DimensionConverter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Add MvRx capabilities to bottomsheetdialog (like BaseMvRxFragment)
 */
abstract class VectorBaseBottomSheetDialogFragment : BottomSheetDialogFragment(), MvRxView {

    private val mvrxViewIdProperty = MvRxViewId()
    final override val mvrxViewId: String by mvrxViewIdProperty
    private lateinit var screenComponent: ScreenComponent

    /* ==========================================================================================
     * View
     * ========================================================================================== */

    @LayoutRes
    abstract fun getLayoutResId(): Int

    private var unBinder: Unbinder? = null

    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val activityViewModelProvider
        get() = ViewModelProvider(requireActivity(), viewModelFactory)

    protected val fragmentViewModelProvider
        get() = ViewModelProvider(this, viewModelFactory)

    /* ==========================================================================================
     * BottomSheetBehavior
     * ========================================================================================== */

    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null

    val vectorBaseActivity: VectorBaseActivity by lazy {
        activity as VectorBaseActivity
    }

    open val showExpanded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(getLayoutResId(), container, false)
        unBinder = ButterKnife.bind(this, view)
        return view
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        unBinder?.unbind()
        unBinder = null
        uiDisposables.clear()
    }

    @CallSuper
    override fun onDestroy() {
        uiDisposables.dispose()
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        screenComponent = DaggerScreenComponent.factory().create(vectorBaseActivity.getVectorComponent(), vectorBaseActivity)
        viewModelFactory = screenComponent.viewModelFactory()
        super.onAttach(context)
        injectWith(screenComponent)
    }

    protected open fun injectWith(injector: ScreenComponent) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        mvrxViewIdProperty.restoreFrom(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume BottomSheet ${this.javaClass.simpleName}")
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
        mvrxViewIdProperty.saveTo(outState)
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

    /* ==========================================================================================
     * Disposable
     * ========================================================================================== */

    private val uiDisposables = CompositeDisposable()

    protected fun Disposable.disposeOnDestroyView(): Disposable {
        uiDisposables.add(this)
        return this
    }

    /* ==========================================================================================
     * Views
     * ========================================================================================== */

    protected fun View.debouncedClicks(onClicked: () -> Unit) {
        clicks()
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onClicked() }
                .disposeOnDestroyView()
    }

    /* ==========================================================================================
     * ViewEvents
     * ========================================================================================== */

    protected fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(observer: (T) -> Unit) {
        viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    observer(it)
                }
                .disposeOnDestroyView()
    }
}
