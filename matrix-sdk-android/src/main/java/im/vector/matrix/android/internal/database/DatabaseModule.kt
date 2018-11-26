package im.vector.matrix.android.internal.database

import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.session.DefaultSession
import io.realm.RealmConfiguration
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import java.io.File

class DatabaseModule : Module {

    override fun invoke(): ModuleDefinition = module {

        scope(DefaultSession.SCOPE) {
            val context = get<Context>()
            val sessionParams = get<SessionParams>()
            val directory = File(context.filesDir, sessionParams.credentials.userId)

            val diskConfiguration = RealmConfiguration.Builder()
                    .directory(directory)
                    .name("disk_store.realm")
                    .deleteRealmIfMigrationNeeded()
                    .build()

            val inMemoryConfiguration = RealmConfiguration.Builder()
                    .directory(directory)
                    .name("in_memory_store.realm")
                    .inMemory()
                    .build()

            DatabaseInstances(
                    disk = Monarchy.Builder().setRealmConfiguration(diskConfiguration).build(),
                    inMemory = Monarchy.Builder().setRealmConfiguration(inMemoryConfiguration).build()
            )
        }

    }.invoke()
}