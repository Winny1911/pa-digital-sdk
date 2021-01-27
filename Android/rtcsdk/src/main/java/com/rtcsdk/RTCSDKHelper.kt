package com.rtcsdk

import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.engineio.client.transports.WebSocket
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext


class RTCSDKHelper {

    private var context: Context? =  null;
    private var clientId:String? = null;
    private var clientSecret:String? = null;
    private var baseApiUri:String? = null;

    private val parentJob = Job()

    private val scope = CoroutineScope(coroutineContext)


    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Default


    private var tokenRequest: TokenRequest?= null;
    private var mSocket : Socket? = null;


    fun startCall (roomId: String) {
        val intent = Intent(context, CallActivity::class.java).apply {
            putExtra("baseApiUri", baseApiUri)
            putExtra("roomId", roomId)
        }
        context!!.startActivity(intent)
    }

    fun joinQueue (person:TicketNumberRequest?) {
        val intent = Intent(context, WaitingRoomActivity::class.java).apply {
            putExtra("clientId", clientId)
            putExtra("clientSecret", clientSecret)
            putExtra("baseApiUri", baseApiUri)
            putExtra("person", person)
        }
        context!!.startActivity(intent)
    }



    fun initModule(context: Context, clientId:String , clientSecret:String, baseApiUri: String)  {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseApiUri = baseApiUri;
        this.context = context;
    }

}