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

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import butterknife.ButterKnife
import butterknife.Unbinder
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.MvRx
import com.bumptech.glide.util.Util.assertMainThread
import im.vector.riotx.core.di.DaggerScreenComponent
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.features.navigation.Navigator
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

abstract class VectorBaseFragment : BaseMvRxFragment(), OnBackPressed, HasScreenInjector {

    // Butterknife unbinder
    private var mUnBinder: Unbinder? = null

    val vectorBaseActivity: VectorBaseActivity by lazy {
        activity as VectorBaseActivity
    }

    /* ==========================================================================================
     * Navigator
     * ========================================================================================== */

    protected lateinit var viewModelFactory: ViewModelProvider.Factory
    protected lateinit var navigator: Navigator
    private lateinit var screenComponent: ScreenComponent

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    override fun onAttach(context: Context) {
        screenComponent = DaggerScreenComponent.factory().create(vectorBaseActivity.getVectorComponent(), vectorBaseActivity)
        navigator = screenComponent.navigator()
        viewModelFactory = screenComponent.viewModelFactory()
        injectWith(injector())
        super.onAttach(context)
    }

    protected open fun injectWith(injector: ScreenComponent) = Unit

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getMenuRes() != -1) {
            setHasOptionsMenu(true)
        }
    }

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutResId(), container, false)
    }

    @LayoutRes
    abstract fun getLayoutResId(): Int

    @CallSuper
    override fun onResume() {
        super.onResume()
        Timber.v("onResume Fragment ${this.javaClass.simpleName}")
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mUnBinder = ButterKnife.bind(this, view)
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        mUnBinder?.unbind()
        mUnBinder = null
    }

    override fun onDestroy() {
        super.onDestroy()
        uiDisposables.dispose()
    }

    override fun injector(): ScreenComponent {
        return screenComponent
    }

    /* ==========================================================================================
     * Restorable
     * ========================================================================================== */

    private val restorables = ArrayList<Restorable>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        restorables.forEach { it.onSaveInstanceState(outState) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        restorables.forEach { it.onRestoreInstanceState(savedInstanceState) }
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun invalidate() {
        //no-ops by default
        Timber.w("invalidate() method has not been implemented")
    }

    protected fun setArguments(args: Parcelable? = null) {
        arguments = args?.let { Bundle().apply { putParcelable(MvRx.KEY_ARG, it) } }
    }

    @MainThread
    protected fun <T : Restorable> T.register(): T {
        assertMainThread()
        restorables.add(this)
        return this
    }

    /* ==========================================================================================
     * Toolbar
     * ========================================================================================== */

    /**
     * Configure the Toolbar.
     */
    protected fun setupToolbar(toolbar: Toolbar) {
        val parentActivity = vectorBaseActivity
        if (parentActivity is ToolbarConfigurable) {
            parentActivity.configure(toolbar)
        }
    }

    /* ==========================================================================================
     * Disposable
     * ========================================================================================== */

    private val uiDisposables = CompositeDisposable()

    protected fun Disposable.disposeOnDestroy(): Disposable {
        uiDisposables.add(this)
        return this
    }

    /* ==========================================================================================
     * MENU MANAGEMENT
     * ========================================================================================== */

    open fun getMenuRes() = -1

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val menuRes = getMenuRes()

        if (menuRes != -1) {
            inflater.inflate(menuRes, menu)
        }
    }

}