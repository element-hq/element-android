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

package im.vector.app.features.crypto.quads

import com.airbnb.mvrx.Uninitialized
import im.vector.app.test.InstantRxRule
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.test
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.securestorage.IntegrityResult
import org.matrix.android.sdk.api.session.securestorage.KeyInfo
import org.matrix.android.sdk.api.session.securestorage.KeyInfoResult
import org.matrix.android.sdk.api.session.securestorage.SecretStorageKeyContent
import org.matrix.android.sdk.api.session.securestorage.SsssPassphrase

private const val IGNORED_PASSPHRASE_INTEGRITY = false
private val KEY_INFO_WITH_PASSPHRASE = KeyInfo(
        id = "id",
        content = SecretStorageKeyContent(passphrase = SsssPassphrase(null, 0, null))
)
private val KEY_INFO_WITHOUT_PASSPHRASE = KeyInfo(id = "id", content = SecretStorageKeyContent(passphrase = null))

class SharedSecureStorageViewModelTest {

    @get:Rule
    val instantRx = InstantRxRule()

    private val stringProvider = FakeStringProvider()
    private val session = FakeSession()

    @Test
    fun `given a key info with passphrase when initialising then step is EnterPassphrase`() {
        givenKey(KEY_INFO_WITH_PASSPHRASE)

        val viewModel = createViewModel()

        viewModel.test().assertState(aViewState(
                hasPassphrase = true,
                step = SharedSecureStorageViewState.Step.EnterPassphrase
        ))
    }

    @Test
    fun `given a key info without passphrase when initialising then step is EnterKey`() {
        givenKey(KEY_INFO_WITHOUT_PASSPHRASE)

        val viewModel = createViewModel()

        viewModel.test().assertState(aViewState(
                hasPassphrase = false,
                step = SharedSecureStorageViewState.Step.EnterKey
        ))
    }

    @Test
    fun `given on EnterKey step when going back then dismisses`() {
        givenKey(KEY_INFO_WITHOUT_PASSPHRASE)

        val viewModel = createViewModel()
        val test = viewModel.test()

        viewModel.handle(SharedSecureStorageAction.Back)

        test.assertEvents(SharedSecureStorageViewEvent.Dismiss)
    }

    @Test
    fun `given on passphrase step when using key then step is EnterKey`() {
        givenKey(KEY_INFO_WITH_PASSPHRASE)
        val viewModel = createViewModel()
        val test = viewModel.test()

        viewModel.handle(SharedSecureStorageAction.UseKey)

        test.assertState(aViewState(
                hasPassphrase = true,
                step = SharedSecureStorageViewState.Step.EnterKey
        ))
    }

    @Test
    fun `given a key info with passphrase and on EnterKey step when going back then step is EnterPassphrase`() {
        givenKey(KEY_INFO_WITH_PASSPHRASE)
        val viewModel = createViewModel()
        val test = viewModel.test()

        viewModel.handle(SharedSecureStorageAction.UseKey)
        viewModel.handle(SharedSecureStorageAction.Back)

        test.assertState(aViewState(
                hasPassphrase = true,
                step = SharedSecureStorageViewState.Step.EnterPassphrase
        ))
    }

    @Test
    fun `given on passphrase step when going back then dismisses`() {
        givenKey(KEY_INFO_WITH_PASSPHRASE)
        val viewModel = createViewModel()
        val test = viewModel.test()

        viewModel.handle(SharedSecureStorageAction.Back)

        test.assertEvents(SharedSecureStorageViewEvent.Dismiss)
    }

    private fun createViewModel() = SharedSecureStorageViewModel(
            SharedSecureStorageViewState(),
            SharedSecureStorageActivity.Args(keyId = null, emptyList(), "alias"),
            stringProvider.instance,
            session
    )

    private fun aViewState(hasPassphrase: Boolean, step: SharedSecureStorageViewState.Step) = SharedSecureStorageViewState(
            ready = true,
            hasPassphrase = hasPassphrase,
            checkingSSSSAction = Uninitialized,
            step = step,
            activeDeviceCount = 0,
            showResetAllAction = false,
            userId = ""
    )

    private fun givenKey(keyInfo: KeyInfo) {
        givenHasAccessToSecrets()
        session.fakeSharedSecretStorageService._defaultKey = KeyInfoResult.Success(keyInfo)
    }

    private fun givenHasAccessToSecrets() {
        session.fakeSharedSecretStorageService.integrityResult = IntegrityResult.Success(passphraseBased = IGNORED_PASSPHRASE_INTEGRITY)
    }
}
