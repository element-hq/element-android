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

package im.vector.app

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ClearCurrentSessionRule : TestWatcher() {
    override fun apply(base: Statement, description: Description): Statement {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            runCatching {
                val holder = (context.applicationContext as VectorApplication).activeSessionHolder
                holder.getSafeActiveSession()?.signOutService()?.signOut(true)
                context.preferencesDataStoreFile(name = "vector_analytics").delete()
                (context.applicationContext as VectorApplication).vectorPreferences.clearPreferences()
                holder.clearActiveSession()
            }
        }
        return super.apply(base, description)
    }
}
