/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.auth

import android.content.Context
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.internal.auth.db.AuthRealmModule
import im.vector.matrix.android.internal.auth.db.RealmSessionParamsStore
import im.vector.matrix.android.internal.auth.db.SessionParamsMapper
import io.realm.RealmConfiguration
import org.koin.dsl.module.module
import java.io.File

class AuthModule {

    val definition = module {

        single {
            DefaultAuthenticator(get(), get(), get()) as Authenticator
        }

        single {
            val context: Context = get()
            val old = File(context.filesDir, "matrix-sdk-auth")

            if (old.exists()) {
                old.renameTo(File(context.filesDir, "matrix-sdk-auth.realm"))
            }

            val mapper = SessionParamsMapper((get()))
            val realmConfiguration = RealmConfiguration.Builder()
                    .name("matrix-sdk-auth.realm")
                    .modules(AuthRealmModule())
                    .deleteRealmIfMigrationNeeded()
                    .build()
            RealmSessionParamsStore(mapper, realmConfiguration) as SessionParamsStore
        }

    }
}
