package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ServerApiTeams {

    @PUT("team/join")
    fun joinTeam(
        @Query("team_id") teamId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Boolean>

    @POST("team/create")
    fun createTeam(
        @Body team: Team,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Int?>

    @DELETE("team/delete")
    fun deleteTeam(
        @Query("team_id") teamId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Boolean>

    @PUT("team/leave")
    fun leaveTeam(
        @Query("team_id") teamId: Int,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Boolean>

    @GET("team/my_teams")
    fun getMyTeams(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<UserTeamsWithInfo>

}