package im.vector.matrix.core.internal.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.converter.moshi.MoshiConverterFactory

class NetworkModule() : Module {

    override fun invoke(): ModuleDefinition = module {

        single {
            AccessTokenInterceptor(get())
        }

        single {
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        }

        single {
            OkHttpClient.Builder()
                    .addInterceptor(get() as AccessTokenInterceptor)
                    .addInterceptor(get() as HttpLoggingInterceptor)
                    .build()
        }

        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

        single {
            MoshiConverterFactory.create(get()) as Converter.Factory
        }

        single {
            CoroutineCallAdapterFactory() as CallAdapter.Factory
        }


    }.invoke()
}