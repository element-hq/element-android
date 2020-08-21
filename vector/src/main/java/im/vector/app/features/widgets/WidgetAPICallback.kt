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

package im.vector.app.features.widgets

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.widgets.WidgetPostAPIMediator
import org.matrix.android.sdk.api.util.JsonDict
import im.vector.app.R
import im.vector.app.core.resources.StringProvider

class WidgetAPICallback(private val postAPIMediator: WidgetPostAPIMediator,
                        private val eventData: JsonDict,
                        private val stringProvider: StringProvider) : MatrixCallback<Any> {

    override fun onFailure(failure: Throwable) {
        postAPIMediator.sendError(stringProvider.getString(R.string.widget_integration_failed_to_send_request), eventData)
    }

    override fun onSuccess(data: Any) {
        postAPIMediator.sendSuccess(eventData)
    }
}
