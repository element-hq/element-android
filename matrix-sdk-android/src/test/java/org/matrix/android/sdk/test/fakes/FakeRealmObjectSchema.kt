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

package org.matrix.android.sdk.test.fakes

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.realm.RealmObjectSchema
import io.realm.RealmObjectSchema.Function

class FakeRealmObjectSchema(
        private val fakeDynamicRealmObject: FakeDynamicRealmObject = FakeDynamicRealmObject()
) {

    val instance: RealmObjectSchema = mockk {
        every { addRealmListField(any(), any<Class<*>>()) } returns this
        every { transform(any()) } returns this
    }

    fun verifyListFieldAdded(fieldName: String, type: Class<*>) = apply {
        verify { instance.addRealmListField(fieldName, type) }
    }

    fun verifyStringTransformation(fieldName: String, transformedInto: String) = apply {
        val transformationSlot = slot<Function>()
        verify { instance.transform(capture(transformationSlot)) }
        transformationSlot.captured.apply(fakeDynamicRealmObject.instance)
        fakeDynamicRealmObject.verifySetString(fieldName, transformedInto)
    }
}
