package com.rtcsdk

class EndpointRepository(private val api : Endpoint) : BaseRepository() {

    suspend fun getToken(request:TokenRequest) : TokenResponse?{

        val token = safeApiCall(
            call = {api.getToken(request).await()},
            errorMessage = "Error Fetching Token"
        )

        return token;

    }

    suspend fun generateTicket(token:String, request:TicketNumberRequest) : TicketNumberResponse?{

        val ticket = safeApiCall(
            call = {api.generateTicket(token, request).await()},
            errorMessage = "Error Generating Ticket"
        )

        return ticket;

    }

    suspend fun getTicket(token:String,ticketId:Int) : TicketResponse?{

        val ticket = safeApiCall(
            call = {api.getTicketInfo(token, ticketId).await()},
            errorMessage = "Error Fetching Ticket Info"
        )

        return ticket;

    }

}