package com.virex.music.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Same public endpoints as Virex Music's web app, for search and
 * playback previews only. No API keys are used or invented here.
 */
interface SearchApi {
    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): SearchResponse

    @GET("api/songs/{id}/suggestions")
    suspend fun suggestions(
        @retrofit2.http.Path("id") id: String,
        @Query("limit") limit: Int = 20
    ): SearchResponse
}

interface LyricsApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("duration") duration: Int? = null
    ): LyricsResponse

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): List<LyricsResponse>
}

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val search: SearchApi = Retrofit.Builder()
        .baseUrl("https://jiosavan-api2.vercel.app/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SearchApi::class.java)

    val lyrics: LyricsApi = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LyricsApi::class.java)
}
