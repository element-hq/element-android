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

package org.matrix.android.sdk.common

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmMigration
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TemporaryRealmConfigurationFactory : TemporaryFolder() {

    private val configurations = HashSet<RealmConfiguration>()

    private var testFailed = false
    private var testName = ""
    private var tempFolder: File? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                testName = description.displayName
                before()
                try {
                    base.evaluate()
                } catch (throwable: Throwable) {
                    testFailed = true
                    throw throwable
                } finally {
                    after()
                }
            }
        }
    }

    @Throws(Throwable::class)
    override fun before() {
        Realm.init(InstrumentationRegistry.getInstrumentation().targetContext)
        super.before()
    }

    override fun after() {
        try {
            for (configuration in configurations) {
                Realm.deleteRealm(configuration)
            }
        } catch (e: IllegalStateException) {
            if (testFailed) {
                throw e
            }
        } finally {
            super.after()
        }
    }

    @Throws(IOException::class)
    override fun create() {
        super.create()
        val tempFolder = File(super.getRoot(), testName)
        check(!(tempFolder.exists() && !tempFolder.delete())) { "Could not delete folder: " + tempFolder.absolutePath }
        check(tempFolder.mkdir()) { "Could not create folder: " + tempFolder.absolutePath }
        this.tempFolder = tempFolder
    }

    fun create(realmFilename: String, assetFilename: String? = null, schemaVersion: Long, module: Any?, migration: RealmMigration? = null): RealmConfiguration {
        val configurationBuilder = RealmConfiguration.Builder()
                .directory(root)
                .name(realmFilename)
                .schemaVersion(schemaVersion)
                .allowWritesOnUiThread(true)

        if (migration != null) {
            configurationBuilder.migration(migration)
        }
        if (module != null) {
            configurationBuilder.modules(module)
        }
        val configuration = configurationBuilder.build()

        if (assetFilename != null) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            copyRealmFromAssets(context, assetFilename, configuration)
        }
        configurations.add(configuration)
        return configuration
    }

    override fun getRoot(): File {
        checkNotNull(tempFolder) { "the temporary folder has not yet been created" }
        return tempFolder!!
    }

    @Throws(IOException::class)
    private fun copyRealmFromAssets(context: Context, assetFileName: String, config: RealmConfiguration) {
        check(!File(config.path).exists()) { "${config.path} already exists" }
        val outFile = File(config.realmDirectory, config.realmFileName)
        copyFileFromAssets(context, assetFileName, outFile)
    }

    @Throws(IOException::class)
    private fun copyFileFromAssets(context: Context, assetPath: String, outFile: File) {
        context.assets.open(assetPath).use { inputStream ->
            FileOutputStream(outFile).use { outputStream ->
                val buf = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buf).also { bytesRead = it } > -1) {
                    outputStream.write(buf, 0, bytesRead)
                }
            }
        }
    }
}
