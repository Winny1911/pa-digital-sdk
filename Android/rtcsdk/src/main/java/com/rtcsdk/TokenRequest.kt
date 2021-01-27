package com.rtcsdk

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("clientId")
    var clientId:String,
    @SerializedName("secret")
    var secret:String
)