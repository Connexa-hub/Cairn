package com.cairn.app.data.backup

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

@Serializable
data class BackupMeta(
    val id: String,
    val filename: String,
    val deviceName: String,
    val sizeBytes: Long,
    val schemaVersion: Int,
    val createdAt: String
)

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val accessToken: String, val tokenType: String = "bearer")

@Serializable
data class UserResponse(val id: String, val email: String, val createdAt: String)

/**
 * Talks only to the user's own optional self-hosted/Render backend, and
 * only when the user explicitly enables encrypted cloud backup. Every
 * payload sent through here is already ciphertext — see [BackupCrypto].
 */
interface BackupApi {

    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearer: String): UserResponse

    @Multipart
    @POST("backup/upload")
    suspend fun upload(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
        @Part("device_name") deviceName: okhttp3.RequestBody
    ): BackupMeta

    @GET("backup/list")
    suspend fun list(@Header("Authorization") bearer: String): List<BackupMeta>

    @Streaming
    @GET("backup/download/{id}")
    suspend fun download(@Header("Authorization") bearer: String, @Path("id") id: String): ResponseBody

    @DELETE("backup/{id}")
    suspend fun delete(@Header("Authorization") bearer: String, @Path("id") id: String): Response<Unit>

    @GET("health")
    suspend fun health(): Map<String, String>
}
