package com.jerboa.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HostInfo(instance: String = DEFAULT_INSTANCE) {
    var api = buildAPI(instance)
        private set

    @Volatile
    var instance = instance
        set(value) {
            api = buildAPI(value)
            field = value
        }

    companion object {
        const val DEFAULT_INSTANCE = "enterprise.lemmy.ml" // TODO change to lemmy.ml
        private const val VERSION = "v3"

        private fun buildApiUrl(instance: String) = "https://$instance/api/$VERSION/"

        private fun buildAPI(instance: String): API {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                        .header("User-Agent", "Jerboa")
                    val newRequest = requestBuilder.build()
                    chain.proceed(newRequest)
                }
                .addInterceptor(interceptor)
                .build()

            return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(buildApiUrl(instance))
                .client(client)
                .build()
                .create(API::class.java)
        }
    }
}
