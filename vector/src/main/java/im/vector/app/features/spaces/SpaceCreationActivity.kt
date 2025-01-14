/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.features.spaces.create.ChoosePrivateSpaceTypeFragment
import im.vector.app.features.spaces.create.ChooseSpaceTypeFragment
import im.vector.app.features.spaces.create.CreateSpaceAction
import im.vector.app.features.spaces.create.CreateSpaceAdd3pidInvitesFragment
import im.vector.app.features.spaces.create.CreateSpaceDefaultRoomsFragment
import im.vector.app.features.spaces.create.CreateSpaceDetailsFragment
import im.vector.app.features.spaces.create.CreateSpaceEvents
import im.vector.app.features.spaces.create.CreateSpaceState
import im.vector.app.features.spaces.create.CreateSpaceViewModel
import im.vector.app.features.spaces.create.SpaceTopology
import im.vector.app.features.spaces.create.SpaceType
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class SpaceCreationActivity : SimpleFragmentActivity() {

    val viewModel: CreateSpaceViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFirstCreation()) {
            when (withState(viewModel) { it.step }) {
                CreateSpaceState.Step.ChooseType -> {
                    navigateToFragment(ChooseSpaceTypeFragment::class.java)
                }
                CreateSpaceState.Step.SetDetails -> {
                    navigateToFragment(ChooseSpaceTypeFragment::class.java)
                }
                CreateSpaceState.Step.AddRooms -> {
                    navigateToFragment(CreateSpaceDefaultRoomsFragment::class.java)
                }
                CreateSpaceState.Step.ChoosePrivateType -> {
                    navigateToFragment(ChoosePrivateSpaceTypeFragment::class.java)
                }
                CreateSpaceState.Step.AddEmailsOrInvites -> {
                    navigateToFragment(CreateSpaceAdd3pidInvitesFragment::class.java)
                }
            }
        }
    }

    override fun initUiAndData() {
        super.initUiAndData()

        viewModel.onEach {
            renderState(it)
        }

        viewModel.observeViewEvents {
            when (it) {
                CreateSpaceEvents.NavigateToDetails -> {
                    navigateToFragment(CreateSpaceDetailsFragment::class.java)
                }
                CreateSpaceEvents.NavigateToChooseType -> {
                    navigateToFragment(ChooseSpaceTypeFragment::class.java)
                }
                CreateSpaceEvents.Dismiss -> {
                    finish()
                }
                CreateSpaceEvents.NavigateToAddRooms -> {
                    navigateToFragment(CreateSpaceDefaultRoomsFragment::class.java)
                }
                CreateSpaceEvents.NavigateToAdd3Pid -> {
                    navigateToFragment(CreateSpaceAdd3pidInvitesFragment::class.java)
                }
                CreateSpaceEvents.NavigateToChoosePrivateType -> {
                    navigateToFragment(ChoosePrivateSpaceTypeFragment::class.java)
                }
                is CreateSpaceEvents.ShowModalError -> {
                    hideWaitingView()
                    MaterialAlertDialogBuilder(this)
                            .setMessage(it.errorMessage)
                            .setPositiveButton(getString(CommonStrings.ok), null)
                            .show()
                }
                is CreateSpaceEvents.FinishSuccess -> {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(RESULT_DATA_CREATED_SPACE_ID, it.spaceId)
                        putExtra(RESULT_DATA_DEFAULT_ROOM_ID, it.defaultRoomId)
                        putExtra(RESULT_DATA_CREATED_SPACE_IS_JUST_ME, it.topology == SpaceTopology.JustMe)
                    })
                    finish()
                }
                CreateSpaceEvents.HideModalLoading -> {
                    hideWaitingView()
                }
                is CreateSpaceEvents.ShowModalLoading -> {
                    showWaitingView(it.message)
                }
            }
        }
    }

    private fun navigateToFragment(fragmentClass: Class<out Fragment>) {
        val frag = supportFragmentManager.findFragmentByTag(fragmentClass.name) ?: fragmentClass.getDeclaredConstructor().newInstance()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(
                        views.container.id,
                        frag,
                        fragmentClass.name
                )
                .commit()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        viewModel.handle(CreateSpaceAction.OnBackPressed)
    }

    private fun renderState(state: CreateSpaceState) {
        val titleRes = when (state.step) {
            CreateSpaceState.Step.ChooseType -> CommonStrings.activity_create_space_title
            CreateSpaceState.Step.SetDetails,
            CreateSpaceState.Step.AddRooms -> {
                if (state.spaceType == SpaceType.Public) CommonStrings.your_public_space
                else CommonStrings.your_private_space
            }
            CreateSpaceState.Step.AddEmailsOrInvites,
            CreateSpaceState.Step.ChoosePrivateType -> CommonStrings.your_private_space
        }
        supportActionBar?.let {
            it.title = getString(titleRes)
        } ?: run {
            setTitle(getString(titleRes))
        }

        if (state.creationResult is Loading) {
            showWaitingView(getString(CommonStrings.create_spaces_loading_message))
        }
    }

    companion object {
        private const val RESULT_DATA_CREATED_SPACE_ID = "RESULT_DATA_CREATED_SPACE_ID"
        private const val RESULT_DATA_DEFAULT_ROOM_ID = "RESULT_DATA_DEFAULT_ROOM_ID"
        private const val RESULT_DATA_CREATED_SPACE_IS_JUST_ME = "RESULT_DATA_CREATED_SPACE_IS_JUST_ME"

        fun newIntent(context: Context): Intent {
            return Intent(context, SpaceCreationActivity::class.java).apply {
                // putExtra(Mavericks.KEY_ARG, SpaceDirectoryArgs(spaceId))
            }
        }

        fun getCreatedSpaceId(data: Intent?): String? {
            return data?.extras?.getString(RESULT_DATA_CREATED_SPACE_ID)
        }

        fun getDefaultRoomId(data: Intent?): String? {
            return data?.extras?.getString(RESULT_DATA_DEFAULT_ROOM_ID)
        }

        fun isJustMeSpace(data: Intent?): Boolean {
            return data?.extras?.getBoolean(RESULT_DATA_CREATED_SPACE_IS_JUST_ME, false) == true
        }
    }
}
