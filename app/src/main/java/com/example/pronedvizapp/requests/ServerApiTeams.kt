package com.example.pronedvizapp.requests

import com.example.pronedvizapp.requests.models.Team
import com.example.pronedvizapp.requests.models.UserTeamsWithInfo
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
    suspend fun joinTeam(
        @Query("team_id") teamId: String,
        @Query("joined_by") joinedBy: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

    @POST("team/create")
    suspend fun createTeam(
        @Body team: Team,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<String?>

    @DELETE("team/delete")
    suspend fun deleteTeam(
        @Query("team_id") teamId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

    @PUT("team/leave")
    suspend fun leaveTeam(
        @Query("team_id") teamId: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

    @PUT("team/move_team_role")
    suspend fun moveTeamRole(
        @Query("team_id") teamId: String,
        @Query("user_id") userId: String,
        @Query("role") role: String,
        @Header("token-authorization") tokenAuthorization: String
    ): Response<Boolean>

    @GET("team/my_teams")
    suspend fun getMyTeams(
        @Header("token-authorization") tokenAuthorization: String
    ): Response<UserTeamsWithInfo>
}