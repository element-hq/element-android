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

package im.vector.riotx.features.settings.devtools

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.yuyh.jsonviewer.library.JsonRecyclerView
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.features.themes.ThemeUtils

class JsonViewerBottomSheetDialog : VectorBaseBottomSheetDialogFragment() {

    override fun getLayoutResId() = R.layout.fragment_jsonviewer

    @BindView(R.id.rv_json)
    lateinit var jsonRecyclerView: JsonRecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        jsonRecyclerView.setKeyColor(ThemeUtils.getColor(requireContext(), R.attr.colorAccent))
        jsonRecyclerView.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.riotx_notice_secondary))
        jsonRecyclerView.setValueNumberColor(ContextCompat.getColor(requireContext(), R.color.riotx_notice_secondary))
        jsonRecyclerView.setValueUrlColor(ThemeUtils.getColor(requireContext(), android.R.attr.textColorLink))
        jsonRecyclerView.setValueNullColor(ContextCompat.getColor(requireContext(), R.color.riotx_notice_secondary))
        jsonRecyclerView.setBracesColor(ThemeUtils.getColor(requireContext(), R.attr.riotx_text_primary))

        val jsonString = arguments?.getString(MvRx.KEY_ARG)
        jsonRecyclerView.bindJson(jsonString)
    }

    companion object {
        fun newInstance(jsonString: String): JsonViewerBottomSheetDialog {
            return JsonViewerBottomSheetDialog().apply {
                setArguments(Bundle().apply { putString(MvRx.KEY_ARG, jsonString) })
            }
        }
    }
}
