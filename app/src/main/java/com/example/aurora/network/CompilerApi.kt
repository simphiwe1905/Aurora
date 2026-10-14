package com.example.aurora.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CompilerApi {

    @GET("languages")
    suspend fun getLanguages(): List<Language>

    @POST("submissions")
    suspend fun createSubmission(
        @Query("base64_encoded") base64Encoded: Boolean = false,
        @Query("wait") wait: Boolean = false,
        @Body request: SubmissionRequest
    ): Response<SubmissionCreated>

    @GET("submissions/{token}")
    suspend fun getSubmission(
        @Path("token") token: String,
        @Query("base64_encoded") base64Encoded: Boolean = false,
        @Query("fields") fields: String =
            "stdout,stderr,compile_output,message,status,time,memory"
    ): SubmissionResult
}