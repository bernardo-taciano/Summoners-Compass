package com.example.summonerscompass.network

import Champion
import ChampionResponse
import ItemResponse
import com.google.gson.GsonBuilder
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private const val BASE_URL = "https://ddragon.leagueoflegends.com/cdn/14.24.1/"

private val gson = GsonBuilder().create()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gson))
    .baseUrl(BASE_URL)
    .build()

interface DataDragonApiService{
    @GET("data/en_US/item.json")
    suspend fun getItems(): ItemResponse

    @GET("img/item/{res}")
    suspend fun getItemSquare(
        @Path("res") res: String
    ): ResponseBody

    @GET("data/en_US/champion.json")
    suspend fun getChampions(): ChampionResponse

    @GET("data/en_US/champion/{champ}.json")
    suspend fun getChampion(
        @Path("champ") champ: String
    ): ChampionResponse

    @GET("img/champion/{res}")
    suspend fun getChampionSquare(
        @Path("res") res: String
    ): ResponseBody
}

object DataDragonApi {
    val retrofitService: DataDragonApiService by lazy {
        retrofit.create(DataDragonApiService::class.java)
    }
}