/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.terms

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class TermsController @Inject constructor(
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<ReviewTermsViewState>() {

    var description: String? = null
    var listener: Listener? = null

    override fun buildModels(data: ReviewTermsViewState?) {
        data ?: return
        val host = this

        when (data.termsList) {
            Uninitialized,
            is Loading -> {
                loadingItem {
                    id("loading")
                }
            }
            is Fail -> {
                errorWithRetryItem {
                    id("errorRetry")
                    text(host.errorFormatter.toHumanReadable(data.termsList.error))
                    listener { host.listener?.retry() }
                }
            }
            is Success -> buildTerms(data.termsList.invoke())
        }
    }

    private fun buildTerms(termsList: List<Term>) {
        val host = this
        settingsSectionTitleItem {
            id("header")
            titleResId(CommonStrings.widget_integration_review_terms)
        }
        termsList.forEach { term ->
            termItem {
                id(term.url)
                name(term.name)
                description(host.description)
                checked(term.accepted)

                clickListener { host.listener?.review(term) }
                checkChangeListener { _, isChecked ->
                    host.listener?.setChecked(term, isChecked)
                }
            }
        }
    }

    interface Listener {
        fun retry()
        fun setChecked(term: Term, isChecked: Boolean)
        fun review(term: Term)
    }
}
