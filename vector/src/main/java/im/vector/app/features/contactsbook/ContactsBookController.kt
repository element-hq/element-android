/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.contactsbook

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

class ContactsBookController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val errorFormatter: ErrorFormatter) : EpoxyController() {

    private var state: ContactsBookViewState? = null

    var callback: Callback? = null

    init {
        requestModelBuild()
    }

    fun setData(state: ContactsBookViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return
        val hasSearch = currentState.searchTerm.isNotEmpty()
        when (val asyncMappedContacts = currentState.mappedContacts) {
            is Uninitialized -> renderEmptyState(false)
            is Loading       -> renderLoading()
            is Success       -> renderSuccess(currentState.filteredMappedContacts, hasSearch, currentState.onlyBoundContacts)
            is Fail          -> renderFailure(asyncMappedContacts.error)
        }
    }

    private fun renderLoading() {
        loadingItem {
            id("loading")
            loadingText(stringProvider.getString(R.string.loading_contact_book))
        }
    }

    private fun renderFailure(failure: Throwable) {
        errorWithRetryItem {
            id("error")
            text(errorFormatter.toHumanReadable(failure))
        }
    }

    private fun renderSuccess(mappedContacts: List<MappedContact>,
                              hasSearch: Boolean,
                              onlyBoundContacts: Boolean) {
        if (mappedContacts.isEmpty()) {
            renderEmptyState(hasSearch)
        } else {
            renderContacts(mappedContacts, onlyBoundContacts)
        }
    }

    private fun renderContacts(mappedContacts: List<MappedContact>, onlyBoundContacts: Boolean) {
        for (mappedContact in mappedContacts) {
            contactItem {
                id(mappedContact.id)
                mappedContact(mappedContact)
                avatarRenderer(avatarRenderer)
            }
            mappedContact.emails
                    .forEachIndexed { index, it ->
                        if (onlyBoundContacts && it.matrixId == null) return@forEachIndexed

                        contactDetailItem {
                            id("${mappedContact.id}-e-$index-${it.email}")
                            threePid(it.email)
                            matrixId(it.matrixId)
                            clickListener {
                                if (it.matrixId != null) {
                                    callback?.onMatrixIdClick(it.matrixId)
                                } else {
                                    callback?.onThreePidClick(ThreePid.Email(it.email))
                                }
                            }
                        }
                    }
            mappedContact.msisdns
                    .forEachIndexed { index, it ->
                        if (onlyBoundContacts && it.matrixId == null) return@forEachIndexed

                        contactDetailItem {
                            id("${mappedContact.id}-m-$index-${it.phoneNumber}")
                            threePid(it.phoneNumber)
                            matrixId(it.matrixId)
                            clickListener {
                                if (it.matrixId != null) {
                                    callback?.onMatrixIdClick(it.matrixId)
                                } else {
                                    callback?.onThreePidClick(ThreePid.Msisdn(it.phoneNumber))
                                }
                            }
                        }
                    }
        }
    }

    private fun renderEmptyState(hasSearch: Boolean) {
        val noResultRes = if (hasSearch) {
            R.string.no_result_placeholder
        } else {
            R.string.empty_contact_book
        }
        noResultItem {
            id("noResult")
            text(stringProvider.getString(noResultRes))
        }
    }

    interface Callback {
        fun onMatrixIdClick(matrixId: String)
        fun onThreePidClick(threePid: ThreePid)
    }
}
