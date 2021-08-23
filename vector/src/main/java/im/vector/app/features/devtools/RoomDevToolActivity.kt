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

package im.vector.app.features.devtools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.createJSonViewerStyleProvider
import kotlinx.parcelize.Parcelize
import org.billcarsonfr.jsonviewer.JSonViewerFragment
import javax.inject.Inject

class RoomDevToolActivity : SimpleFragmentActivity(), RoomDevToolViewModel.Factory,
        FragmentManager.OnBackStackChangedListener {

    @Inject lateinit var viewModelFactory: RoomDevToolViewModel.Factory
    @Inject lateinit var colorProvider: ColorProvider

    //    private lateinit var viewModel: RoomDevToolViewModel
    private val viewModel: RoomDevToolViewModel by viewModel()

    override fun getTitleRes() = R.string.dev_tools_menu_name

    override fun getMenuRes() = R.menu.menu_devtools

    private var currentDisplayMode: RoomDevToolViewState.Mode? = null

    @Parcelize
    data class Args(
            val roomId: String
    ) : Parcelable

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    override fun create(initialState: RoomDevToolViewState): RoomDevToolViewModel {
        return viewModelFactory.create(initialState)
    }

    override fun initUiAndData() {
        super.initUiAndData()
        viewModel.subscribe(this) {
            renderState(it)
        }

        viewModel.observeViewEvents {
            when (it) {
                DevToolsViewEvents.Dismiss             -> finish()
                is DevToolsViewEvents.ShowAlertMessage -> {
                    MaterialAlertDialogBuilder(this)
                            .setMessage(it.message)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    Unit
                }
                is DevToolsViewEvents.ShowSnackMessage -> showSnackbar(it.message)
            }.exhaustive
        }
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    private fun renderState(it: RoomDevToolViewState) {
        if (it.displayMode != currentDisplayMode) {
            when (it.displayMode) {
                RoomDevToolViewState.Mode.Root                 -> {
                    val classJava = RoomDevToolFragment::class.java
                    val tag = classJava.name
                    if (supportFragmentManager.findFragmentByTag(tag) == null) {
                        replaceFragment(R.id.container, RoomDevToolFragment::class.java)
                    } else {
                        supportFragmentManager.popBackStack()
                    }
                }
                RoomDevToolViewState.Mode.StateEventDetail     -> {
                    val frag = JSonViewerFragment.newInstance(
                            jsonString = it.selectedEventJson ?: "",
                            initialOpenDepth = -1,
                            wrap = true,
                            styleProvider = createJSonViewerStyleProvider(colorProvider)
                    )
                    navigateTo(frag)
                }
                RoomDevToolViewState.Mode.StateEventList,
                RoomDevToolViewState.Mode.StateEventListByType -> {
                    val frag = createFragment(RoomDevToolStateEventListFragment::class.java, Bundle().toMvRxBundle())
                    navigateTo(frag)
                }
                RoomDevToolViewState.Mode.EditEventContent     -> {
                    val frag = createFragment(RoomDevToolEditFragment::class.java, Bundle().toMvRxBundle())
                    navigateTo(frag)
                }
                is RoomDevToolViewState.Mode.SendEventForm     -> {
                    val frag = createFragment(RoomDevToolSendFormFragment::class.java, Bundle().toMvRxBundle())
                    navigateTo(frag)
                }
            }
            currentDisplayMode = it.displayMode
            invalidateOptionsMenu()
        }

        when (it.modalLoading) {
            is Loading    -> showWaitingView()
            is Success    -> hideWaitingView()
            is Fail       -> {
                hideWaitingView()
            }
            Uninitialized -> {
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        if (item.itemId == R.id.menuItemEdit) {
            viewModel.handle(RoomDevToolAction.MenuEdit)
            return true
        }
        if (item.itemId == R.id.menuItemSend) {
            viewModel.handle(RoomDevToolAction.MenuItemSend)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        viewModel.handle(RoomDevToolAction.OnBackPressed)
    }

    private fun navigateTo(fragment: Fragment) {
        val tag = fragment.javaClass.name
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.container, fragment, tag)
                    .addToBackStack(tag)
                    .commit()
        } else {
            if (!supportFragmentManager.popBackStackImmediate(tag, 0)) {
                supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.container, fragment, tag)
                        .addToBackStack(tag)
                        .commit()
            }
        }
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        currentDisplayMode = null
        super.onDestroy()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean = withState(viewModel) { state ->
        menu?.forEach {
            val isVisible = when (it.itemId) {
                R.id.menuItemEdit -> {
                    state.displayMode is RoomDevToolViewState.Mode.StateEventDetail
                }
                R.id.menuItemSend -> {
                    state.displayMode is RoomDevToolViewState.Mode.EditEventContent
                            || state.displayMode is RoomDevToolViewState.Mode.SendEventForm
                }
                else              -> true
            }
            it.isVisible = isVisible
        }
        return@withState true
    }

    companion object {

        fun intent(context: Context, roomId: String): Intent {
            return Intent(context, RoomDevToolActivity::class.java).apply {
                putExtra(MvRx.KEY_ARG, Args(roomId))
            }
        }
    }

    override fun onBackStackChanged() = withState(viewModel) { state ->
        updateToolBar(state)
    }

    private fun updateToolBar(state: RoomDevToolViewState) {
        val title = when (state.displayMode) {
            RoomDevToolViewState.Mode.Root                 -> {
                getString(getTitleRes())
            }
            RoomDevToolViewState.Mode.StateEventList       -> {
                getString(R.string.dev_tools_state_event)
            }
            RoomDevToolViewState.Mode.StateEventDetail     -> {
                state.selectedEvent?.type
            }
            RoomDevToolViewState.Mode.EditEventContent     -> {
                getString(R.string.dev_tools_edit_content)
            }
            RoomDevToolViewState.Mode.StateEventListByType -> {
                state.currentStateType ?: ""
            }
            is RoomDevToolViewState.Mode.SendEventForm     -> {
                getString(
                        if (state.displayMode.isState) R.string.dev_tools_send_custom_state_event
                        else R.string.dev_tools_send_custom_event
                )
            }
        }

        supportActionBar?.let {
            it.title = title
        } ?: run {
            setTitle(title)
        }
        invalidateOptionsMenu()
    }
}
