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

package im.vector.app.test.fakes

import android.graphics.drawable.Drawable
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import org.matrix.android.sdk.api.util.MatrixItem

class FakeLocationPinProvider {

    val instance = mockk<LocationPinProvider>(relaxed = true)

    fun givenCreateForMatrixItem(matrixItem: MatrixItem, expectedDrawable: Drawable) {
        every { instance.create(matrixItem, captureLambda()) } answers { lambda<(Drawable) -> Unit>().invoke(expectedDrawable) }
    }
}
