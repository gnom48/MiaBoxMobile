package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.TranscriptionTask
import com.example.pronedvizapp.requests.models.UsersCalls
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerApiCalls {

    @Multipart
    @POST("calls/add_call_info")
    suspend fun addCallInfo(
        @Part file: MultipartBody.Part,
        @Part("info") info: RequestBody,
        @Part("phone_number") phoneNumber: RequestBody,
        @Part("date_time") dateTime: RequestBody,
        @Part("contact_name") contactName: RequestBody,
        @Part("length_seconds") lengthSeconds: RequestBody,
        @Part("call_type") callType: RequestBody,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Int?>

    @GET("calls/get_all_calls")
    suspend fun getAllCalls(
        @Query("user_id") userId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<List<UsersCalls>>

    @GET("calls/get_call_record_file/{record_id}")
    suspend fun getCallRecordFile(
        @Path("record_id") recordId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<ByteArray>

    @GET("calls/order_call_transcription/{record_id}")
    suspend fun orderCallTranscription(
        @Path("record_id") recordId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<TranscriptionTask>

}