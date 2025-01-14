/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.contactsbook

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

class ContactsBookController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val errorFormatter: ErrorFormatter
) : EpoxyController() {

    private var state: ContactsBookViewState? = null

    var callback: Callback? = null

    fun setData(state: ContactsBookViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return
        when (val asyncMappedContacts = currentState.mappedContacts) {
            is Uninitialized -> renderEmptyState(false)
            is Loading -> renderLoading()
            is Success -> renderSuccess(currentState)
            is Fail -> renderFailure(asyncMappedContacts.error)
        }
    }

    private fun renderLoading() {
        val host = this
        loadingItem {
            id("loading")
            loadingText(host.stringProvider.getString(CommonStrings.loading_contact_book))
        }
    }

    private fun renderFailure(failure: Throwable) {
        val host = this
        errorWithRetryItem {
            id("error")
            text(host.errorFormatter.toHumanReadable(failure))
        }
    }

    private fun renderSuccess(state: ContactsBookViewState) {
        val mappedContacts = state.filteredMappedContacts

        if (mappedContacts.isEmpty()) {
            renderEmptyState(state.searchTerm.isNotEmpty())
        } else {
            renderContacts(mappedContacts, state.onlyBoundContacts)
        }
    }

    private fun renderContacts(mappedContacts: List<MappedContact>, onlyBoundContacts: Boolean) {
        val host = this
        for (mappedContact in mappedContacts) {
            contactItem {
                id(mappedContact.id)
                mappedContact(mappedContact)
                avatarRenderer(host.avatarRenderer)
            }
            mappedContact.emails
                    .forEachIndexed { index, email ->
                        if (onlyBoundContacts && email.matrixId == null) return@forEachIndexed

                        contactDetailItem {
                            id("${mappedContact.id}-e-$index-${email.email}")
                            threePid(email.email)
                            matrixId(email.matrixId)
                            clickListener {
                                if (email.matrixId != null) {
                                    host.callback?.onMatrixIdClick(email.matrixId)
                                } else {
                                    host.callback?.onThreePidClick(ThreePid.Email(email.email))
                                }
                            }
                        }
                    }
            mappedContact.msisdns
                    .forEachIndexed { index, msisdn ->
                        if (onlyBoundContacts && msisdn.matrixId == null) return@forEachIndexed

                        contactDetailItem {
                            id("${mappedContact.id}-m-$index-${msisdn.phoneNumber}")
                            threePid(msisdn.phoneNumber)
                            matrixId(msisdn.matrixId)
                            clickListener {
                                if (msisdn.matrixId != null) {
                                    host.callback?.onMatrixIdClick(msisdn.matrixId)
                                } else {
                                    host.callback?.onThreePidClick(ThreePid.Msisdn(msisdn.phoneNumber))
                                }
                            }
                        }
                    }
        }
    }

    private fun renderEmptyState(hasSearch: Boolean) {
        val host = this
        val noResultRes = if (hasSearch) {
            CommonStrings.no_result_placeholder
        } else {
            CommonStrings.empty_contact_book
        }
        noResultItem {
            id("noResult")
            text(host.stringProvider.getString(noResultRes))
        }
    }

    interface Callback {
        fun onMatrixIdClick(matrixId: String)
        fun onThreePidClick(threePid: ThreePid)
    }
}
