package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Note
import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.UserStatuses
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ServerApiTeams {

    @POST("team/join")
    fun joinTeam(
        @Query("team_id") teamId: Int,
        @Query("joined_by") joinedBy: Int,
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

    @PUT("team/move_team_role")
    fun moveTeamRole(
        @Query("team_id") teamId: Int,
        @Query("user_id") userId: Int,
        @Query("role") role: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Call<Boolean>

    @GET("team/my_teams")
    fun getMyTeams(
        @Header("token-authorization") tokenAuthorization: String
    ): Call<UserTeamsWithInfo>

}