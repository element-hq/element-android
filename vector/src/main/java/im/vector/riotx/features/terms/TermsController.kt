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
package im.vector.riotx.features.terms

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.R
import im.vector.riotx.features.discovery.settingsSectionTitle
import javax.inject.Inject

class TermsController @Inject constructor() : TypedEpoxyController<List<Term>>() {

    var description: String? = null
    var listener: Listener? = null

    override fun buildModels(data: List<Term>?) {
        data?.let {
            settingsSectionTitle {
                id("header")
                titleResId(R.string.widget_integration_review_terms)
            }
            it.forEach { term ->
                termItem {
                    id(term.url)
                    name(term.name)
                    description(description)
                    checked(term.accepted)

                    clickListener(View.OnClickListener { listener?.review(term) })
                    checkChangeListener { _, isChecked ->
                        listener?.setChecked(term, isChecked)
                    }
                }
            }
        }
        //TODO error mgmt
    }

    interface Listener {
        fun setChecked(term: Term, isChecked: Boolean)
        fun review(term: Term)
    }
}
