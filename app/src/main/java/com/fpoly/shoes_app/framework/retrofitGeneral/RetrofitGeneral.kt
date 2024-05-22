package com.fpoly.shoes_app.framework.retrofitGeneral

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitGeneral {
        private const val BASE_URL = "http://10.0.2.2:8000/api/"
        val retrofitInstance: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
