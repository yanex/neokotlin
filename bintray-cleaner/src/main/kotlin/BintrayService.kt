package org.jetbrains.neokotlin

import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.*

internal interface BintrayService {
    @GET("packages/{owner}/{repository}/{packageName}")
    fun getPackage(
            @Path("owner") owner: String,
            @Path("repository") repository: String,
            @Path("packageName") packageName: String
    ): Call<BintrayPackage>

    @GET("packages/{owner}/{repository}/{packageName}/versions/{version}")
    fun getVersion(
            @Header("Authorization") credentials: String,
            @Path("owner") owner: String,
            @Path("repository") repository: String,
            @Path("packageName") packageName: String,
            @Path("version") version: String
    ): Call<BintrayVersion>

    @DELETE("packages/{owner}/{repository}/{packageName}/versions/{version}")
    fun deleteVersion(
            @Header("Authorization") credentials: String,
            @Path("owner") owner: String,
            @Path("repository") repository: String,
            @Path("packageName") packageName: String,
            @Path("version") version: String
    ): Call<Unit>
}

data class BintrayPackage(val versions: List<String>)
data class BintrayVersion(val published: Boolean, val created: Date)