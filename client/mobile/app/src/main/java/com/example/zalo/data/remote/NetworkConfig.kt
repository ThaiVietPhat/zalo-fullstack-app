package com.example.zalo.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConfig @Inject constructor() {
    // Correct Railway Backend URL provided by user
    val railwayBackendUrl = "https://zalo-fullstack-app-production.up.railway.app/" 
    val railwayWsUrl = "wss://zalo-fullstack-app-production.up.railway.app/ws"
    
    fun getRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(railwayBackendUrl + "api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
