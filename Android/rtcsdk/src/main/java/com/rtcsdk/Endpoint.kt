package com.rtcsdk

import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface Endpoint {

    @Headers("Content-Type: application/json")
    @POST("auth/login")
    fun getToken(@Body request:TokenRequest) : Deferred<Response<TokenResponse>>

    @Headers("Content-Type: application/json")
    @POST("client/pa/ticket/generate")
    fun generateTicket(@Header("Authorization") token:String, @Body request:TicketNumberRequest) :  Deferred<Response<TicketNumberResponse>>

    @Headers("Content-Type: application/json")
    @GET("client/pa/ticket/{ticketId}")
    fun getTicketInfo(@Header("Authorization") token:String ,@Path("ticketId") ticketId:Int) : Deferred<Response<TicketResponse>>
}