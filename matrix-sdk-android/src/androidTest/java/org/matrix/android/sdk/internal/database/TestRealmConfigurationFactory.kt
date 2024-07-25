/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.database

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
import java.io.InputStream
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Based on https://github.com/realm/realm-java/blob/master/realm/realm-library/src/testUtils/java/io/realm/TestRealmConfigurationFactory.java
 */
class TestRealmConfigurationFactory : TemporaryFolder() {
    private val map: Map<RealmConfiguration, Boolean> = ConcurrentHashMap()
    private val configurations = Collections.newSetFromMap(map)
    @get:Synchronized private var isUnitTestFailed = false
    private var testName = ""
    private var tempFolder: File? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                setTestName(description)
                before()
                try {
                    base.evaluate()
                } catch (throwable: Throwable) {
                    setUnitTestFailed()
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
            // Only throws the exception caused by deleting the opened Realm if the test case itself doesn't throw.
            if (!isUnitTestFailed) {
                throw e
            }
        } finally {
            // This will delete the temp directory.
            super.after()
        }
    }

    @Throws(IOException::class)
    override fun create() {
        super.create()
        tempFolder = File(super.getRoot(), testName)
        check(!(tempFolder!!.exists() && !tempFolder!!.delete())) { "Could not delete folder: " + tempFolder!!.absolutePath }
        check(tempFolder!!.mkdir()) { "Could not create folder: " + tempFolder!!.absolutePath }
    }

    override fun getRoot(): File {
        checkNotNull(tempFolder) { "the temporary folder has not yet been created" }
        return tempFolder!!
    }

    /**
     * To be called in the [.apply].
     */
    protected fun setTestName(description: Description) {
        testName = description.displayName
    }

    @Synchronized
    fun setUnitTestFailed() {
        isUnitTestFailed = true
    }

    // This builder creates a configuration that is *NOT* managed.
    // You have to delete it yourself.
    private fun createConfigurationBuilder(): RealmConfiguration.Builder {
        return RealmConfiguration.Builder().directory(root)
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
    }

    fun createConfiguration(
            name: String,
            key: String?,
            module: Any,
            schemaVersion: Long,
            migration: RealmMigration?
    ): RealmConfiguration {
        val builder = createConfigurationBuilder()
        builder
                .directory(root)
                .name(name)
                .apply {
                    if (key != null) {
                        encryptionKey(key.decodeHex())
                    }
                }
                .modules(module)
                // Allow writes on UI
                .allowWritesOnUiThread(true)
                .schemaVersion(schemaVersion)
                .apply {
                    migration?.let { migration(it) }
                }
        val configuration = builder.build()
        configurations.add(configuration)
        return configuration
    }

    // Copies a Realm file from assets to temp dir
    @Throws(IOException::class)
    fun copyRealmFromAssets(context: Context, realmPath: String, newName: String) {
        val config = RealmConfiguration.Builder()
                .directory(root)
                .name(newName)
                .build()
        copyRealmFromAssets(context, realmPath, config)
    }

    @Throws(IOException::class)
    fun copyRealmFromAssets(context: Context, realmPath: String, config: RealmConfiguration) {
        check(!File(config.path).exists()) { String.format(Locale.ENGLISH, "%s exists!", config.path) }
        val outFile = File(config.realmDirectory, config.realmFileName)
        copyFileFromAssets(context, realmPath, outFile)
    }

    @Throws(IOException::class)
    fun copyFileFromAssets(context: Context, assetPath: String?, outFile: File?) {
        var stream: InputStream? = null
        var os: FileOutputStream? = null
        try {
            stream = context.assets.open(assetPath!!)
            os = FileOutputStream(outFile)
            val buf = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buf).also { bytesRead = it } > -1) {
                os.write(buf, 0, bytesRead)
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (ignore: IOException) {
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (ignore: IOException) {
                }
            }
        }
    }
}
