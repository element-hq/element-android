/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.e2einfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import org.matrix.android.sdk.api.extensions.orFalse

@AndroidEntryPoint
class MessageE2EInfoActivity : SimpleFragmentActivity() {

    override fun getTitleRes() = R.string.message_e2e_info_title

    override fun getMenuRes() = R.menu.menu_crypto_info

    val sharedViewModel: CryptoInfoViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar(views.toolbar)
                .setTitle(title)
                .allowBack(useCross = true)

        if (isFirstCreation()) {
            val fragmentArgs: EncryptedMessageInfoArg = intent?.extras?.getParcelable(Mavericks.KEY_ARG) ?: return
            replaceFragment(CryptoInfoFragment::class.java, fragmentArgs)
            updateTitle(getString(R.string.message_e2e_info_title))
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val backstackCount = supportFragmentManager.backStackEntryCount
            val title = if (backstackCount == 0) {
                getString(R.string.message_e2e_info_title)
            } else {
                val frag = supportFragmentManager.getBackStackEntryAt(backstackCount - 1).name
                if (frag == ReviewRequestFragment::class.java.name) {
                    getString(R.string.message_e2e_room_key_request_title)
                } else {
                    getString(R.string.message_e2e_info_title)
                }
            }
            updateTitle(title)
            invalidateOptionsMenu()
        }

        sharedViewModel.onEach {
            invalidateOptionsMenu()
        }

        sharedViewModel.observeViewEvents { event ->
            when (event) {
                is CryptoInfoEvents.NavigateToRequestReview -> {
                    event.incomingRoomKeyRequest.requestId?.let {
                        pushFragment(ReviewRequestFragment::class.java, IncomingKeyRequestArgs(it))
                        updateTitle(getString(R.string.message_e2e_room_key_request_title))
                    }
                }
                CryptoInfoEvents.NavigateToFilter           -> {
                    pushFragment(SearchCryptoInfoFragment::class.java, null)
                    updateTitle(getString(R.string.message_e2e_info_title))
                }
            }
        }
    }

    private fun updateTitle(title: String) {
        toolbar?.setTitle(title)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        withState(sharedViewModel) {
            menu?.findItem(R.id.menu_e2e_info_filter)?.isVisible = supportFragmentManager.backStackEntryCount == 0 &&
                    (it.e2eInfo.invoke()?.incomingRoomKeyRequest?.isEmpty()?.not().orFalse() ||
                            it.e2eInfo.invoke()?.sharedWithUsers?.isEmpty()?.not().orFalse())
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_e2e_info_filter) {
            sharedViewModel.handle(CryptoInfoAction.EnableFilter)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun replaceFragment(fragmentClass: Class<out Fragment>, fragmentArgs: Parcelable?) {
        addFragment(views.container, fragmentClass, fragmentArgs)
    }

    private fun pushFragment(fragmentClass: Class<out Fragment>, fragmentArgs: Parcelable?) {
        val frag = supportFragmentManager.findFragmentByTag(fragmentClass.name) ?: createFragment(fragmentClass, fragmentArgs)
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(views.container.id,
                        frag,
                        fragmentClass.name
                )
                .addToBackStack(fragmentClass.name)
                .commit()
    }

    companion object {

        fun getIntent(context: Context, roomId: String, eventId: String): Intent {
            return Intent(context, MessageE2EInfoActivity::class.java).also {
                it.putExtra(Mavericks.KEY_ARG, EncryptedMessageInfoArg(roomId = roomId, eventId = eventId))
            }
        }
    }
}
